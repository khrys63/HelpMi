-- STATUS
UPDATE config_values SET label_en = 'Open',        label_bg = 'Отворен'      WHERE category = 'STATUS' AND code = 'OPEN';
UPDATE config_values SET label_en = 'In progress',  label_bg = 'В процес'     WHERE category = 'STATUS' AND code = 'IN_PROGRESS';
UPDATE config_values SET label_en = 'Stand by',     label_bg = 'На изчакване' WHERE category = 'STATUS' AND code = 'STAND_BY';
UPDATE config_values SET label_en = 'Resolved',     label_bg = 'Решен'        WHERE category = 'STATUS' AND code = 'RESOLVED';
UPDATE config_values SET label_en = 'Closed',       label_bg = 'Затворен'     WHERE category = 'STATUS' AND code = 'CLOSED';
UPDATE config_values SET label_en = 'Cancelled',    label_bg = 'Отменен'      WHERE category = 'STATUS' AND code = 'CANCELLED';

-- PRIORITY
UPDATE config_values SET label_en = 'Low',      label_bg = 'Ниска'    WHERE category = 'PRIORITY' AND code = 'LOW';
UPDATE config_values SET label_en = 'Medium',   label_bg = 'Средна'   WHERE category = 'PRIORITY' AND code = 'MEDIUM';
UPDATE config_values SET label_en = 'High',     label_bg = 'Висока'   WHERE category = 'PRIORITY' AND code = 'HIGH';
UPDATE config_values SET label_en = 'Critical', label_bg = 'Критична' WHERE category = 'PRIORITY' AND code = 'CRITICAL';

-- TYPE
UPDATE config_values SET label_en = 'Bug',     label_bg = 'Грешка'      WHERE category = 'TYPE' AND code = 'BUG';
UPDATE config_values SET label_en = 'Feature', label_bg = 'Функционалност' WHERE category = 'TYPE' AND code = 'FEATURE';
UPDATE config_values SET label_en = 'Task',    label_bg = 'Задача'      WHERE category = 'TYPE' AND code = 'TASK';
UPDATE config_values SET label_en = 'Support', label_bg = 'Поддръжка'   WHERE category = 'TYPE' AND code = 'SUPPORT';

-- LINK_TYPE (label + inverse_label)
UPDATE config_values SET
    label_en = 'Relates to',   label_bg = 'Свързан с',
    inverse_label_en = 'Relates to',   inverse_label_bg = 'Свързан с'
    WHERE category = 'LINK_TYPE' AND code = 'RELATES_TO';

UPDATE config_values SET
    label_en = 'Depends on',   label_bg = 'Зависи от',
    inverse_label_en = 'Required by',  inverse_label_bg = 'Изисква се от'
    WHERE category = 'LINK_TYPE' AND code = 'DEPENDS_ON';

UPDATE config_values SET
    label_en = 'Blocks',       label_bg = 'Блокира',
    inverse_label_en = 'Blocked by',   inverse_label_bg = 'Блокиран от'
    WHERE category = 'LINK_TYPE' AND code = 'BLOCKS';

UPDATE config_values SET
    label_en = 'Duplicates',   label_bg = 'Дублира',
    inverse_label_en = 'Duplicated by', inverse_label_bg = 'Дублиран от'
    WHERE category = 'LINK_TYPE' AND code = 'DUPLICATES';

-- PROJECT_ROLE
UPDATE config_values SET label_en = 'Manager', label_bg = 'Мениджър' WHERE category = 'PROJECT_ROLE' AND code = 'MANAGER';
UPDATE config_values SET label_en = 'Member',  label_bg = 'Член'     WHERE category = 'PROJECT_ROLE' AND code = 'MEMBER';
