CREATE TABLE card (
	id INT AUTO_INCREMENT PRIMARY KEY, -- 총 78개
	name VARCHAR(50) NOT NULL,
	arcana ENUM('MAJOR', 'MINOR') NOT NULL, -- 메이저/마이너 구분
	suit ENUM('NONE', 'WANDS', 'CUPS', 'SWORDS', 'PENTACLES') DEFAULT 'NONE', -- 마이너 카드 원소
	card_number INT NOT NULL, -- 메이저(0~21), 마이너(1~14, 11:Page, 12:Knight, 13:Queen, 14:King)
	-- Ex. 페이지 완드 -> suit='WANDS', card_number=11

	-- 조합 가능한 원자단위
    symbols JSON NOT NULL, -- 객체 배열
    themes JSON NOT NULL,  -- 문자열 배열
    upright_energy VARCHAR(255) NOT NULL,
    reversed_energy VARCHAR(255) NOT NULL,
    tension VARCHAR(255) NOT NULL
);