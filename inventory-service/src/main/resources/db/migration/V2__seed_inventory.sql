-- Demo catalogue so the flow can be exercised immediately after start-up.
INSERT INTO inventory_items (sku, name, available_quantity, updated_at, version)
VALUES ('SKY-GLASS-55', 'Sky Glass 55"', 10, now(), 0),
       ('SKY-STREAM-PUCK', 'Sky Stream Puck', 100, now(), 0),
       ('SKYQ-BOX-2TB', 'Sky Q 2TB Box', 25, now(), 0),
       ('SKY-BROADBAND-HUB', 'Sky Broadband Hub', 50, now(), 0),
       ('SKY-SOUNDBOX', 'Sky Soundbox', 0, now(), 0);
