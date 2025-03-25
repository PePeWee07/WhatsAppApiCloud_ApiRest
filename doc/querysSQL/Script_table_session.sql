SELECT * FROM public.chat_sessions
ORDER BY id ASC 

-- buscar sesiones activas por dia y contarlas
SELECT COUNT(*) 
FROM chat_sessions
WHERE start_time::date = '2025-03-21';

-- Json all chat_session for Phone
SELECT uc.*,
       json_agg(cs) AS sessions
FROM user_chat uc
LEFT JOIN chat_sessions cs ON uc.phone = cs.phone
WHERE uc.phone = '593983439289'
GROUP BY uc.id;

-- Session activas por usuario only(1)
SELECT COUNT(*) AS active_sessions
FROM chat_sessions
WHERE phone = '593983439289'
  AND start_time >= (NOW() - INTERVAL '24 hours');


