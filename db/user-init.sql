CREATE TABLE account (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	email VARCHAR(255) NOT NULL UNIQUE,
	password VARCHAR(255) NOT NULL,
	nickname VARCHAR(255) NOT NULL,
	-- 회원 무료 이용권. 가입일(created_at, KST)부터 7일 단위 주기로 리셋된다.
	-- free_reading_cycle_start 가 현재 주기 시작일이 아니면 사용량 0으로 간주
	free_reading_used INT NOT NULL DEFAULT 0,
	free_reading_cycle_start DATE,
	created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
