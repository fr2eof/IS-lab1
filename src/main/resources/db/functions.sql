-- Database functions for special operations

-- Function to calculate average heart count
CREATE OR REPLACE FUNCTION get_average_heart_count()
RETURNS DECIMAL AS $$
BEGIN
    RETURN (SELECT AVG(heart_count) FROM space_marines);
END;
$$ LANGUAGE plpgsql;

-- Function to count marines with health less than threshold
CREATE OR REPLACE FUNCTION count_marines_by_health(health_threshold INTEGER)
RETURNS INTEGER AS $$
BEGIN
    RETURN (SELECT COUNT(*) FROM space_marines WHERE health < health_threshold);
END;
$$ LANGUAGE plpgsql;

-- Function to find marines by name containing substring
CREATE OR REPLACE FUNCTION find_marines_by_name(name_substring VARCHAR)
RETURNS TABLE(
    id INTEGER,
    name VARCHAR,
    health INTEGER,
    heart_count INTEGER,
    category VARCHAR,
    weapon_type VARCHAR,
    chapter_name VARCHAR,
    creation_date TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        sm.id,
        sm.name,
        sm.health,
        sm.heart_count,
        sm.category::VARCHAR,
        sm.weapon_type::VARCHAR,
        c.name as chapter_name,
        sm.creation_date
    FROM space_marines sm
    LEFT JOIN chapters c ON sm.chapter_id = c.id
    WHERE sm.name ILIKE '%' || name_substring || '%';
END;
$$ LANGUAGE plpgsql;

-- Function to create new chapter
CREATE OR REPLACE FUNCTION create_new_chapter(chapter_name VARCHAR, marines_count INTEGER)
RETURNS INTEGER AS $$
DECLARE
    new_chapter_id INTEGER;
BEGIN
    INSERT INTO chapters (name, marines_count)
    VALUES (chapter_name, marines_count)
    RETURNING id INTO new_chapter_id;
    
    RETURN new_chapter_id;
END;
$$ LANGUAGE plpgsql;

-- Function to remove marine from chapter
CREATE OR REPLACE FUNCTION remove_marine_from_chapter(chapter_id_param BIGINT)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE chapters 
    SET marines_count = marines_count - 1 
    WHERE id = chapter_id_param AND marines_count > 0;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;
