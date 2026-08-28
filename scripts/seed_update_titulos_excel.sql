-- Script de actualización de títulos académicos según la matriz oficial de la Escuela Provincias Unidas

UPDATE sga_principal.personas p
SET titulo_academico = 'Licenciado en Educación Informática'
FROM sga_principal.usuarios u
WHERE p.id_usuario = u.id_usuario AND u.username = 'jsjimenezt';

UPDATE sga_principal.personas p
SET titulo_academico = 'Magíster (Msc.)'
FROM sga_principal.usuarios u
WHERE p.id_usuario = u.id_usuario AND u.username IN ('amoreira', 'earteaga');

UPDATE sga_principal.personas p
SET titulo_academico = CASE 
    WHEN p.genero = 'FEMENINO' THEN 'Licenciada en Educación'
    ELSE 'Licenciado en Educación'
END
FROM sga_principal.usuarios u
WHERE p.id_usuario = u.id_usuario 
  AND u.username IN ('aalcivar', 'cmacias', 'gcarrera', 'gvera', 'gruiz', 'jvera', 'kgonzalez', 'nlitardo', 'pguagaje', 'rcansiong', 'rmunoz')
  AND (p.titulo_academico IS NULL OR p.titulo_academico IN ('LIC.', 'Licenciada', 'Licenciado', ''));
