-- 관리자 2단계 인증(OTP 앱). 비밀값은 AES-256 암호화해서 저장하고, 켜진 시각(otp_enabled_at)이 있어야 사용 중
alter table members add column otp_secret varchar(255);
alter table members add column otp_enabled_at timestamp(6) with time zone;
alter table members add column otp_last_step bigint;                -- 마지막으로 쓴 코드 (같은 코드 다시 쓰기 막기)
alter table members add column otp_failures int not null default 0; -- 켜진 뒤 연달아 틀린 횟수 (10번이면 잠김)