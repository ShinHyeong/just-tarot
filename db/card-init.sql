CREATE TABLE card (
	id INT PRIMARY KEY,
	name VARCHAR(50) NOT NULL,
	name_ko VARCHAR(50) NOT NULL,
	arcana ENUM('MAJOR', 'MINOR') NOT NULL,
	suit ENUM('NONE', 'WANDS', 'CUPS', 'SWORDS', 'PENTACLES') DEFAULT 'NONE',
	card_number INT NOT NULL,

    symbols JSON NOT NULL,
    themes JSON NOT NULL,
    upright_energy VARCHAR(255) NOT NULL,
    reversed_energy VARCHAR(255) NOT NULL,
    axis VARCHAR(255) NOT NULL,

    UNIQUE KEY uk_card_identity (arcana, suit, card_number)
) DEFAULT CHARSET = utf8mb4;