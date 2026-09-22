INSERT INTO recipes (
    id, created_at, is_deleted,
    name, description, cooking_time_minutes, difficulty, serving_size, instruction_text
) VALUES 
(gen_random_uuid(), current_timestamp, false, 'Phở Bò Truyền Thống', 'Món phở truyền thống của Việt Nam với nước dùng ngọt thanh từ xương.', 120, 'Medium', 4, '1. Ninh xương bò 2-3 tiếng với thảo mộc.\n2. Chuẩn bị bánh phở và rau sống.\n3. Thái thịt bò thật mỏng.\n4. Trụng phở, xếp thịt và chan nước dùng thật nóng.'),
(gen_random_uuid(), current_timestamp, false, 'Gỏi Cuốn Tôm Thịt', 'Gỏi cuốn tôm thịt thanh mát chấm cùng tương đen hoặc nước mắm tỏi ớt.', 30, 'Easy', 2, '1. Luộc chín tôm và thịt lợn, thái mỏng.\n2. Rửa sạch rau thơm, xà lách.\n3. Làm ướt bánh tráng, xếp tôm, thịt, bún và rau rồi cuốn chặt tay.\n4. Pha nước chấm ăn kèm.'),
(gen_random_uuid(), current_timestamp, false, 'Bún Chả Hà Nội', 'Thịt lợn nướng chả thơm lừng ăn kèm bún, rau sống và nước chấm chua ngọt.', 45, 'Medium', 3, '1. Băm nhỏ và thái lát thịt lợn, ướp gia vị (nước mắm, đường, hành tỏi băm).\n2. Nướng chả trên than hoa cho vàng đều.\n3. Pha nước chấm chua ngọt với đu đủ, cà rốt chua.\n4. Dọn ra mẹt và thưởng thức cùng bún.');
