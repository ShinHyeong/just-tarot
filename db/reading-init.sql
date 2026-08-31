CREATE TABLE reading (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	user_id BIGINT NOT NULL,
	question VARCHAR(255) NOT NULL,
	created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

	FOREIGN KEY (user_id) REFERENCES account(id),

	INDEX idx_user_id_created_at (user_id, created_at DESC)
);

CREATE TABLE reading_card (
	reading_id BIGINT NOT NULL,
	card_id INT NOT NULL,
	is_reversed BOOLEAN NOT NULL DEFAULT FALSE,

	PRIMARY KEY (reading_id, card_id),

	FOREIGN KEY (reading_id) REFERENCES reading(id),
	FOREIGN KEY (card_id) REFERENCES card(id)
);