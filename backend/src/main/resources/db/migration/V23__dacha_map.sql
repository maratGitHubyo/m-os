-- Default dacha map for all existing game sessions
UPDATE game_sessions
SET map_image_url = '/maps/dacha.png'
WHERE map_image_url IS NULL OR map_image_url = '';
