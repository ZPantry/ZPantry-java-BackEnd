package com.zpantry.subscription.service;

import com.zpantry.subscription.api.SubscriptionDtos.*;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {
    public static final String MEAL_SUGGESTION = "MEAL_SUGGESTION", OCR = "OCR";
    private final JdbcTemplate jdbc;
    private final PayosGateway payos;
    private final Clock clock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    public SubscriptionService(JdbcTemplate jdbc, PayosGateway payos) { this.jdbc = jdbc; this.payos = payos; }
    public List<PlanResponse> plans() { return List.of(new PlanResponse("Z_FREE","Z-Free",0,0,"10 gợi ý/ngày · 3 lần OCR/tháng"), new PlanResponse("Z_PLUS","Z-Plus",49000,30,"Gợi ý không giới hạn · 5 lần OCR/ngày")); }
    public SubscriptionResponse current(UUID user) { var row = active(user); String plan = row == null ? "Z_FREE" : row.plan; return new SubscriptionResponse(plan, row == null ? "FREE" : row.status, row == null ? null : row.started, row == null ? null : row.expires, row != null && row.autoRenew, quotas(user, plan)); }
    @Transactional public void consume(UUID user, String feature) {
        String plan = active(user) == null ? "Z_FREE" : "Z_PLUS"; int limit = limit(plan, feature); if (limit < 0) return;
        LocalDate start = feature.equals(MEAL_SUGGESTION) || plan.equals("Z_PLUS") ? LocalDate.now(clock) : YearMonth.now(clock).atDay(1);
        String sql = "INSERT INTO subscription_usage(id,created_at,user_id,feature,period_start,used_count) VALUES (?,?,?,?,?,1) "
                + "ON CONFLICT(user_id,feature,period_start) DO UPDATE SET used_count=subscription_usage.used_count+1,updated_at=EXCLUDED.created_at "
                + "WHERE subscription_usage.used_count < ? RETURNING used_count";
        Integer count;
        try { count = jdbc.queryForObject(sql, Integer.class, UUID.randomUUID(), timestampNow(), user, feature, start, limit); }
        catch (org.springframework.dao.EmptyResultDataAccessException ignored) { count = null; }
        if (count == null) throw new SubscriptionLimitExceededException(feature, limit);
    }
    @Transactional public CheckoutResponse checkout(UUID user, String provider) {
        String normalized = provider.toUpperCase(Locale.ROOT); if (!"PAYOS".equals(normalized)) throw new IllegalArgumentException("Only PayOS is currently available.");
        UUID id=UUID.randomUUID(); long orderCode=System.currentTimeMillis(); String order="ZP-"+orderCode; Timestamp now=timestampNow(); jdbc.update("INSERT INTO payment_transactions(id,created_at,user_id,provider,merchant_order_id,amount_vnd,status) VALUES(?,?,?,?,?,?,?)",id,now,user,normalized,order,49000,"PENDING");
        try { String url=payos.create(orderCode,49000,"ZPantry ZPlus"); jdbc.update("UPDATE payment_transactions SET checkout_url=?,updated_at=? WHERE id=?",url,now,id); return new CheckoutResponse(id.toString(), normalized, "PENDING", url); } catch(RuntimeException failure){jdbc.update("UPDATE payment_transactions SET status='FAILED',failure_reason=?,updated_at=? WHERE id=?","PayOS link creation failed",now,id);throw failure;}
    }
    @Transactional public void cancel(UUID user) { Timestamp now=timestampNow(); jdbc.update("UPDATE user_subscriptions SET status='CANCELLED', cancelled_at=?, updated_at=? WHERE user_id=? AND status='ACTIVE' AND (expires_at IS NULL OR expires_at>?)",now,now,user,now); }
    @Transactional public boolean confirmPayment(String transactionId, String providerReference) { try { UUID paymentId=UUID.fromString(transactionId); var rows=jdbc.query("SELECT user_id,status FROM payment_transactions WHERE id=? FOR UPDATE",(ResultSet r,int n)->new Object[]{r.getObject(1,UUID.class),r.getString(2)},paymentId); if(rows.isEmpty()||!"PENDING".equals(rows.getFirst()[1]))return false; UUID user=(UUID)rows.getFirst()[0];Instant now=Instant.now(clock),expiry=now.plus(30,ChronoUnit.DAYS);Timestamp nowTimestamp=Timestamp.from(now);jdbc.update("UPDATE payment_transactions SET status='PAID',provider_transaction_id=?,paid_at=?,updated_at=? WHERE id=?",providerReference,nowTimestamp,nowTimestamp,paymentId);jdbc.update("UPDATE user_subscriptions SET status='EXPIRED',updated_at=? WHERE user_id=? AND status='ACTIVE'",nowTimestamp,user);jdbc.update("INSERT INTO user_subscriptions(id,created_at,user_id,plan_code,status,started_at,expires_at,provider,auto_renew) VALUES(?,?,?,?,?,?,?,?,true)",UUID.randomUUID(),nowTimestamp,user,"Z_PLUS","ACTIVE",nowTimestamp,Timestamp.from(expiry),"WEBHOOK");return true;}catch(IllegalArgumentException invalid){return false;} }
    private List<QuotaResponse> quotas(UUID u,String plan){return List.of(quota(u,plan,MEAL_SUGGESTION),quota(u,plan,OCR));}
    private QuotaResponse quota(UUID u,String p,String f){int l=limit(p,f); LocalDate reset=f.equals(MEAL_SUGGESTION)||p.equals("Z_PLUS")?LocalDate.now(clock).plusDays(1):YearMonth.now(clock).plusMonths(1).atDay(1);LocalDate start=f.equals(MEAL_SUGGESTION)||p.equals("Z_PLUS")?LocalDate.now(clock):YearMonth.now(clock).atDay(1);var counts=jdbc.query("SELECT used_count FROM subscription_usage WHERE user_id=? AND feature=? AND period_start=?",(ResultSet r,int n)->r.getInt(1),u,f,start);int x=counts.isEmpty()?0:counts.getFirst();return new QuotaResponse(f,l<0?null:l,x,l<0?null:Math.max(0,l-x),reset);}
    private int limit(String p,String f){return p.equals("Z_PLUS")?(f.equals(OCR)?5:-1):(f.equals(OCR)?3:10);}
    private Active active(UUID u){var rows=jdbc.query("SELECT plan_code,status,started_at,expires_at,auto_renew FROM user_subscriptions WHERE user_id=? AND status='ACTIVE' AND expires_at>? ORDER BY expires_at DESC LIMIT 1",(ResultSet r,int n)->new Active(r.getString(1),r.getString(2),r.getTimestamp(3).toInstant(),r.getTimestamp(4).toInstant(),r.getBoolean(5)),u,timestampNow());return rows.isEmpty()?null:rows.getFirst();}
    private Timestamp timestampNow(){ return Timestamp.from(Instant.now(clock)); }
    private record Active(String plan,String status,Instant started,Instant expires,boolean autoRenew){}
}
