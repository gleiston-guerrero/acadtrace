import urllib.request
import json
import os

username = os.environ.get('SGA_ADMIN_USERNAME')
password = os.environ.get('SGA_ADMIN_PASSWORD')
if not username or not username.strip() or not password or not password.strip():
    raise SystemExit('Debe definir SGA_ADMIN_USERNAME y SGA_ADMIN_PASSWORD con valores no vacíos.')

# Script para actualizar todos los docentes en la base de datos de producción AWS
url_login = 'http://192.0.2.1:8080/api/auth/login'
req = urllib.request.Request(
    url_login, 
    data=json.dumps({'username': username, 'password': password}).encode(),
    headers={'Content-Type': 'application/json'}
)

token = None
try:
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode())
        token = res.get('token')
except Exception:
    print("Error al autenticar; no se muestran detalles para proteger las credenciales.")

if token:
    print("Autenticado con éxito en AWS!")
    headers = {'Authorization': f'Bearer {token}', 'Content-Type': 'application/json'}
    
    # Obtener usuarios
    req_u = urllib.request.Request('http://192.0.2.1:8080/api/usuarios', headers={'Authorization': f'Bearer {token}'})
    with urllib.request.urlopen(req_u) as resp:
        usuarios = json.loads(resp.read().decode())
    
    docentes_datos = {
        'gcarrera': {'nombres': 'GLADYS MARIA', 'apellidos': 'CARRERA ZAMBRANO', 'cedula': '0927061945', 'genero': 'FEMENINO', 'tituloAcademico': 'Licenciada en Educación', 'especializacion': 'Educación Básica (3ro EGB)'},
        'gvera': {'nombres': 'GLADYS ROXANA', 'apellidos': 'VERA INTRIAGO', 'cedula': '2144802234', 'genero': 'FEMENINO', 'tituloAcademico': 'Licenciada en Educación', 'especializacion': 'Educación Básica (2do EGB)'},
        'cmacias': {'nombres': 'CRUZ MARIA', 'apellidos': 'MACIAS RODRIGUEZ', 'cedula': '0136392172', 'genero': 'FEMENINO', 'tituloAcademico': 'Licenciada en Educación', 'especializacion': 'Educación Básica (4to EGB)'},
        'pguagaje': {'nombres': 'PIEDAD ALICIA', 'apellidos': 'GUAGAJE LLANO', 'cedula': '1327349534', 'genero': 'FEMENINO', 'tituloAcademico': 'Licenciada en Educación', 'especializacion': 'Educación Inicial (1ero EGB)'},
        'jvera': {'nombres': 'JORGE EMILIO', 'apellidos': 'VERA TRIVIÑO', 'cedula': '1003855424', 'genero': 'MASCULINO', 'tituloAcademico': 'Licenciado en Educación', 'especializacion': 'Lengua y Literatura'},
        'rmunoz': {'nombres': 'ROGELIO LIZARDO', 'apellidos': 'MUÑOZ OLIVO', 'cedula': '1449986320', 'genero': 'MASCULINO', 'tituloAcademico': 'Licenciado en Educación', 'especializacion': 'Ciencias Naturales'},
        'nlitardo': {'nombres': 'NEXABEL LILIANA', 'apellidos': 'LITARDO FIGUEROA', 'cedula': '0528539638', 'genero': 'FEMENINO', 'tituloAcademico': 'Licenciada en Educación', 'especializacion': 'Educación Básica (5to EGB)'},
        'jsjimenezt': {'nombres': 'JAIRO SEGUNDO', 'apellidos': 'JIMENEZ TOVAR', 'cedula': '1308414943', 'genero': 'MASCULINO', 'tituloAcademico': 'Lic. Educación Informática', 'especializacion': 'Estudios Sociales'},
        'rcansiong': {'nombres': 'RITA CAROLINA', 'apellidos': 'CANSIONG VELEZ', 'cedula': '0720498344', 'genero': 'FEMENINO', 'tituloAcademico': 'Licenciada en Educación', 'especializacion': 'Inglés'},
        'kgonzalez': {'nombres': 'KAREN STEFANIA', 'apellidos': 'GONZALEZ SABANDO', 'cedula': '0534857503', 'genero': 'FEMENINO', 'tituloAcademico': 'Licenciada en Educación', 'especializacion': 'Educación Artística (ECA)'},
        'amoreira': {'nombres': 'ADRIAN ANTONIO', 'apellidos': 'MOREIRA ALAVA', 'cedula': '1938313796', 'genero': 'MASCULINO', 'tituloAcademico': 'Magíster (Msc.)', 'especializacion': 'Matemática'},
        'aalcivar': {'nombres': 'ALBA ALEXANDRA', 'apellidos': 'ALCIVAR OSORIO', 'cedula': '0723394714', 'genero': 'FEMENINO', 'tituloAcademico': 'Licenciada en Educación', 'especializacion': 'Educación Básica'},
    }
    
    for u in usuarios:
        uname = u.get('username')
        uid = u.get('idUsuario')
        if uname in docentes_datos:
            d = docentes_datos[uname]
            # Consultar si ya tiene persona
            id_persona = None
            try:
                req_p = urllib.request.Request(f'http://192.0.2.1:8080/api/personas/usuario/{uid}', headers={'Authorization': f'Bearer {token}'})
                with urllib.request.urlopen(req_p) as resp_p:
                    pdata = json.loads(resp_p.read().decode())
                    id_persona = pdata.get('idPersona')
            except Exception:
                pass
            
            payload = {
                'idUsuario': uid,
                'cedula': d['cedula'],
                'nombres': d['nombres'],
                'apellidos': d['apellidos'],
                'genero': d['genero'],
                'tituloAcademico': d['tituloAcademico'],
                'especializacion': d['especializacion']
            }
            
            try:
                if id_persona:
                    req_up = urllib.request.Request(f'http://192.0.2.1:8080/api/personas/{id_persona}', data=json.dumps(payload).encode(), headers=headers, method='PUT')
                else:
                    req_up = urllib.request.Request('http://192.0.2.1:8080/api/personas', data=json.dumps(payload).encode(), headers=headers, method='POST')
                
                with urllib.request.urlopen(req_up) as resp_res:
                    print(f'✅ ACTUALIZADO COMPLETO: {uname} -> {d["nombres"]} {d["apellidos"]} ({d["tituloAcademico"]})')
            except Exception:
                print(f'❌ ERROR AL ACTUALIZAR {uname}; no se muestran detalles de la respuesta.')
