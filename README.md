![GameBox](logo.svg)
# GameBox

Un hub de minijuegos dentro de Minecraft. Snake, Apaga la Luz, Buscaminas,
Sudoku, Memory, 2048, Simon, Mastermind, 10x10 y Flappy Bird — todo en un
mod de Fabric, con récords locales, tema claro/oscuro, y una clasificación
de servidor opcional para competir con puntuaciones.

## Índice

- [Minijuegos](#minijuegos)
- [Características](#características)
- [Requisitos](#requisitos)
- [Instalación (jugadores)](#instalación-jugadores)
- [Clasificación de servidor](#clasificación-de-servidor)
- [Contribuir](#contribuir)
- [Licencia](#licencia)

## Minijuegos

| Juego | Dificultades | Descripción |
|---|---|---|
| Snake | Sí | El clásico juego de la serpiente. |
| Apaga la Luz | Sí | Apaga todas las luces del tablero. |
| Buscaminas | Sí | Despeja el tablero sin detonar ninguna mina. |
| Sudoku | Sí | Rellena la cuadrícula 9x9 sin repetir números. |
| Memory | Sí | Encuentra todas las parejas de cartas. |
| 2048 | No | Combina fichas hasta llegar a 2048. |
| Simon | Sí | Repite la secuencia de colores. |
| Mastermind | Sí | Adivina el código secreto de colores. |
| 10x10 | No | Encaja piezas en una cuadrícula, tipo Tetris sin caída. |
| Flappy Bird | No | Vuela entre los huecos de las tuberías. |

## Características

- **10 minijuegos** con lógica de juego separada de la interfaz, para que
  cada uno sea fácil de mantener y extender.
- **Récords locales** por jugador y por dificultad, con verificación de
  integridad del archivo de guardado.
- **Estadísticas agregadas**: total de partidas jugadas y tu juego favorito.
- **Tema claro/oscuro** configurable.
- **Ayuda in-game** ("Cómo jugar") para cada minijuego.
- **Clasificación de servidor opcional** (ver más abajo).
- Mod **exclusivamente de cliente** por defecto: no necesitas instalarlo en
  ningún servidor para jugar, y no rompe la compatibilidad con servidores
  vanilla o con otros mods.

## Requisitos

- [Fabric Loader](https://fabricmc.net/use/)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 25 o superior

> La versión exacta de Minecraft y de Fabric API necesarias dependen del
> archivo que descargues — consulta la sección
> [Releases](../../releases) de este repositorio: cada release indica en
> su descripción para qué versión de Minecraft fue compilado.

## Instalación (jugadores)

1. Instala [Fabric Loader](https://fabricmc.net/use/) para tu versión de
   Minecraft.
2. Descarga la versión de **Fabric API** compatible con tu versión de
   Minecraft desde [Modrinth](https://modrinth.com/mod/fabric-api) o
   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
3. Descarga el `.jar` de GameBox correspondiente a tu versión de Minecraft
   desde la sección [Releases](../../releases) de este repositorio.
4. Copia ambos archivos `.jar` (Fabric API y GameBox) a la carpeta `mods` de
   tu instalación de Minecraft.
5. Inicia el juego con el perfil de Fabric. Pulsa **G** en cualquier momento
   dentro de una partida para abrir el menú de GameBox.

## Clasificación de servidor

GameBox incluye un sistema opcional de clasificación compartida:

- Si te conectas a un servidor **que no tiene GameBox instalado**, el mod
  funciona exactamente igual que en modo un jugador: solo con récords
  locales, sin ningún cambio de comportamiento.
- Si te conectas a un servidor **que sí tiene GameBox instalado**, cada
  minijuego muestra automáticamente un panel con el Top 5 de puntuaciones
  del servidor para esa dificultad, incluyendo la skin de los jugadores que
  estén conectados en ese momento.
- Para que un servidor ofrezca esta función, su operador solo necesita
  instalar el mismo `.jar` de GameBox (para su versión de Minecraft) en la
  carpeta `mods` del servidor — no requiere ninguna configuración adicional.


Cada minijuego sigue el mismo patrón: una clase de **lógica pura** (sin
dependencias de Minecraft, fácil de testear de forma aislada), una clase de
**pantalla** que dibuja y gestiona la entrada, y una clase de **récord**
serializable con Gson.

## Contribuir

Las incidencias y las pull requests son bienvenidas. Si añades un minijuego
nuevo, intenta seguir el mismo patrón de separación lógica/pantalla que el
resto del proyecto.

## Licencia

Este proyecto está bajo la licencia MIT — consulta el archivo
[LICENSE](LICENSE) para más detalles.