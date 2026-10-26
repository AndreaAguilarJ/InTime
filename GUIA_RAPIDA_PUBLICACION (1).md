# 🚀 GUÍA RÁPIDA: PREPARAR APP PARA GOOGLE PLAY

## PASO 1: Crear Keystore (5 minutos)

Abre terminal en la carpeta del proyecto y ejecuta:

```bash
keytool -genkey -v -keystore momentum-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias momentum-key
```

**Guarda bien las contraseñas que ingreses - las necesitarás siempre!**

## PASO 2: Configurar gradle.properties (2 minutos)

Agrega al final del archivo `gradle.properties`:

```properties
MOMENTUM_STORE_FILE=momentum-release-key.jks
MOMENTUM_STORE_PASSWORD=tu_password_del_keystore
MOMENTUM_KEY_ALIAS=momentum-key
MOMENTUM_KEY_PASSWORD=tu_password_de_la_key
```

**⚠️ IMPORTANTE:** Agrega `momentum-release-key.jks` a tu `.gitignore`

## PASO 3: Generar App Bundle (1 minuto)

```bash
./gradlew bundleRelease
```

El archivo estará en: `app/build/outputs/bundle/release/app-release.aab`

## PASO 4: Probar el Release (5 minutos)

```bash
./gradlew installRelease
```

Prueba la app en tu dispositivo para verificar que todo funciona.

## PASO 5: Crear Cuenta Google Play Console (30 minutos + $25 USD)

1. Ve a: https://play.google.com/console/signup
2. Paga la tarifa única de $25 USD
3. Completa tu perfil de desarrollador

## PASO 6: Subir la App a Internal Testing (15 minutos)

1. En Play Console → "Crear aplicación"
2. Completa información básica (nombre, idioma, etc.)
3. Ve a "Testing interno"
4. Sube el archivo `.aab`
5. Completa la información requerida

## PASO 7: Preparar Recursos Gráficos

**Necesitas:**
- [ ] Ícono 512x512 px (PNG)
- [ ] Al menos 2 screenshots (1080x1920 o similar)
- [ ] Gráfico destacado 1024x500 px (opcional pero recomendado)

## PASO 8: Completar Información Obligatoria

En Google Play Console, completa:
- [ ] Descripción corta (80 caracteres)
- [ ] Descripción completa (hasta 4000 caracteres)
- [ ] Categoría: "Productividad"
- [ ] Política de Privacidad URL
- [ ] Clasificación de contenido (cuestionario)
- [ ] Seguridad de datos (declaración)
- [ ] Declaración de permisos

## PASO 9: Justificar Permisos Sensibles

Google te pedirá explicar y demostrar (con video) el uso de:
- `PACKAGE_USAGE_STATS` - Para estadísticas de uso
- `READ_CONTACTS` - Para modo minimalista
- `SYSTEM_ALERT_WINDOW` - Para bloqueo de apps
- `CALL_PHONE` / `SMS` - Para modo minimalista

## PASO 10: Enviar a Revisión

Una vez completado todo:
1. Revisa el checklist de Google Play Console
2. Click en "Enviar a revisión"
3. Espera 1-7 días para aprobación

---

## ✅ CHECKLIST FINAL

Antes de enviar, verifica:
- [ ] App funciona perfectamente en release
- [ ] No hay crashes
- [ ] Todos los permisos están justificados
- [ ] Política de Privacidad publicada
- [ ] Screenshots de buena calidad
- [ ] Descripción completa y atractiva
- [ ] Clasificación de contenido completada
- [ ] Información de contacto correcta

---

## 🆘 PROBLEMAS COMUNES

**Error al generar AAB:**
- Verifica que el keystore existe
- Revisa las contraseñas en gradle.properties

**App rechazada por permisos:**
- Proporciona video mostrando cada permiso en uso
- Explica claramente por qué es necesario

**Política de Privacidad:**
- Usa la plantilla en `PRIVACY_POLICY.md`
- Publícala en GitHub Pages o tu sitio web
- Incluye la URL en Play Console

---

## 📞 CONTACTO Y SOPORTE

Si tienes dudas durante el proceso:
1. Consulta la [Ayuda de Google Play Console](https://support.google.com/googleplay/android-developer)
2. Revisa `CHECKLIST_GOOGLE_PLAY.md` para más detalles
3. Usa la comunidad de desarrolladores de Android

**¡Buena suerte con tu lanzamiento! 🚀**

