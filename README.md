# DataCollector - Aplicación de Rastreo Android

Esta aplicación recolecta datos del dispositivo (batería, ubicación, modelo, etc.) periódicamente y los envía a un servidor PHP.

## 📱 Cómo compilar y generar el APK (Método Local Recomendado)

Dado que la compilación en la nube puede ser inestable sin configuración avanzada, el método más seguro es usar **Android Studio** en tu computadora.

### Requisitos previos
1. Descargar e instalar [Android Studio](https://developer.android.com/studio) (es gratuito).
2. Tener instalado Java Development Kit (JDK) 17 (Android Studio suele incluirlo).

### Pasos para generar el APK

1. **Abrir el proyecto:**
   - Abre Android Studio.
   - Selecciona **"Open"** (Abrir).
   - Navega hasta la carpeta donde tienes este código (`c:\xampp\htdocs\cmovil2`) y selecciona la carpeta raíz.
   - Espera a que Android Studio indexe y descargue las dependencias (esto puede tardar unos minutos la primera vez).

2. **Configurar la IP del servidor:**
   - Abre el archivo: `app/src/main/java/com/example/datacollector/api/ApiService.kt`.
   - Busca la línea: `private const val BASE_URL = "http://10.0.2.2/cmovil2/api/"`.
   - **Si usas el emulador:** Déjalo como `10.0.2.2` (esto apunta a tu localhost).
   - **Si usas un celular real:** Cambia `10.0.2.2` por la dirección IP local de tu PC (ejemplo: `192.168.1.XX`).

3. **Compilar:**
   - En el menú superior, ve a **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
   - Espera a que termine el proceso.
   - Aparecerá una notificación "Build APK(s): APK(s) generated successfully". Haz clic en **"locate"** para abrir la carpeta con el archivo `.apk`.

4. **Instalar:**
   - Copia el archivo APK a tu teléfono y ábrelo para instalar.

## 🔧 Configuración del Backend (XAMPP)

1. Asegúrate de que Apache y MySQL estén corriendo en XAMPP.
2. Importa el archivo `database.sql` en phpMyAdmin para crear la tabla.
3. Verifica que los archivos PHP estén en `c:\xampp\htdocs\cmovil2\`.

## 🐛 Solución de problemas comunes

- **Error "SDK location not found":** Android Studio creará automáticamente un archivo `local.properties` con la ruta al SDK. Si no lo hace, asegúrate de tener el SDK instalado desde el "SDK Manager".
- **Permisos:** La app pedirá permisos de ubicación y teléfono al abrirse. Debes aceptarlos para que funcione.
