# Guía de Importación Masiva de Productos Fitosanitarios

## ✅ Sistema Completado

He implementado un sistema completo de importación masiva de productos desde CSV que requiere **mínima intervención** de tu parte.

## 🚀 Cómo Usar el Sistema

### 1. **Acceder a la Importación**
- Ve a la pantalla "Catálogo de Productos"
- En la barra superior, busca el ícono de **carga/upload** (📤)
- Toca el ícono para abrir el diálogo de importación

### 2. **Seleccionar el Archivo CSV**
- El sistema está configurado para el formato del **Vademecum_Senasa.csv**
- Selecciona tu archivo CSV desde el dispositivo
- El sistema detectará automáticamente el formato

### 3. **Configurar Opciones**
- **Omitir productos incompletos**: Si está activado, solo importa productos con todos los datos
- **Si está desactivado**: Te mostrará un reporte de productos con datos faltantes

### 4. **Proceso de Importación**
El sistema procesará automáticamente:
- ✅ **6000+ productos** del Vademecum SENASA
- ✅ **Detección automática** de formulaciones nuevas
- ✅ **Clasificación inteligente** por tipo de aplicación
- ✅ **Validación de datos** y reporte de incompletos

### 5. **Gestión de Formulaciones Nuevas**
Si se detectan formulaciones no existentes:
- Se abrirá un diálogo para cada formulación nueva
- Podrás elegir:
  - **Tipo**: Líquido o Sólido
  - **Posición**: Dónde ubicarla en el orden de mezcla
- **Vista previa** del orden actualizado
- Opciones para **omitir** formulaciones si no las necesitas

## 📊 Formato del CSV Soportado

El sistema está optimizado para el formato del Vademecum SENASA:
```
NUM. REG.;MARCA;ACTIVOS;BANDA TOX;APTITUDES;FORMULACION;TIPO DE APLICACION
```

### Mapeo Automático:
- **NUM. REG.** → Número de Registro SENASA
- **MARCA** → Nombre Comercial
- **ACTIVOS** → Principio Activo + Concentración (extraída automáticamente)
- **BANDA TOX** → Banda Toxicológica
- **APTITUDES** → Tipo de Producto (IN-Insecticida, HE-Herbicida, etc.)
- **FORMULACION** → Formulación del producto
- **TIPO DE APLICACION** → PULVERIZACION o ESPARCIDO

## 🎯 Características Inteligentes

### **Productos Sin Tipo Definido**
- Aparecen automáticamente en **ambas pestañas** (Pulverización y Esparcido)
- No necesitas duplicar manualmente

### **Detección de Formulaciones**
- Compara automáticamente con las 12 formulaciones existentes
- Solo te pregunta por las **nuevas** que no están en tu lista
- Mantiene el orden actual de formulaciones

### **Validación Inteligente**
- Reporta productos con datos faltantes
- Extrae concentraciones automáticamente del principio activo
- Clasifica tipos de productos por códigos SENASA

### **Gestión de Errores**
- Reporte detallado de productos no importados
- Estadísticas completas del proceso
- Opción de reintentar con diferentes configuraciones

## 📱 Integración con la App

### **Pestañas de Productos**
- **Pulverización**: Productos que requieren formulación y mezcla
- **Esparcido**: Productos de aplicación directa
- **Productos sin tipo**: Aparecen en ambas pestañas automáticamente

### **Búsqueda Mejorada**
- Busca por nombre comercial y principio activo
- Filtrado automático por pestaña seleccionada
- Acceso rápido a miles de productos

### **Formulaciones Dinámicas**
- Mantiene las 12 formulaciones existentes
- Agrega nuevas solo cuando es necesario
- Control total sobre el orden de mezcla

## 🔧 Archivos Creados/Modificados

### Nuevos Archivos:
1. `CsvImportManager.kt` - Motor de importación
2. `CsvImportDialog.kt` - Interfaz de importación
3. `NewFormulationDialog.kt` - Gestión de formulaciones nuevas
4. `ProductsScreenWithImport.kt` - Integración completa

### Archivos Modificados:
1. `ProductsRepository.kt` - Métodos para formulaciones dinámicas
2. `ProductsViewModel.kt` - Integración con importación
3. `ProductsScreen.kt` - Botón de importación y ayuda actualizada

## 💡 Recomendaciones de Uso

1. **Primera Importación**: Usa "Omitir productos incompletos" = **DESACTIVADO** para ver qué datos faltan
2. **Formulaciones**: Revisa cuidadosamente el orden de mezcla antes de agregar nuevas formulaciones
3. **Backup**: Considera hacer backup de la base de datos antes de importaciones masivas
4. **Rendimiento**: La importación puede tardar varios minutos para 6000+ productos

## 🎉 Resultado Final

Después de la importación tendrás:
- ✅ **6000+ productos fitosanitarios** disponibles instantáneamente
- ✅ **Búsqueda rápida** por nombre o principio activo
- ✅ **Clasificación automática** en pestañas
- ✅ **Formulaciones organizadas** según tu preferencia
- ✅ **Datos completos** incluyendo bandas toxicológicas y registros SENASA

**¡Tu app ahora tiene una base de datos completa de productos fitosanitarios sin necesidad de carga manual!**
