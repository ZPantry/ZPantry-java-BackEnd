package com.zpantry.subscription.service;
public class SubscriptionLimitExceededException extends RuntimeException { public SubscriptionLimitExceededException(String feature,int limit){super(feature+" quota exhausted (limit: "+limit+").");} }
