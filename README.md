# PacSpanz

PacSpanz è un minigioco 2D arcade a labirinto scritto in Java 17 con Swing. Il giocatore attraversa una griglia, raccoglie pellet, attiva pellet energia temporanei e deve evitare quattro fantasmi con comportamenti diversi.

Il progetto è personale e non usa risorse ufficiali, nomi ufficiali, personaggi ufficiali o riferimenti diretti protetti.

## Requisiti

- Java 17
- Maven 3.x
- Swing
- Nessuna libreria grafica esterna
- Nessun JavaFX

## Struttura cartelle

```text
PacSpanz/
|-- pom.xml
|-- README.md
`-- src/
    `-- main/
        |-- java/
        |   `-- it/
        |       `-- pacspanz/
        |           |-- Main.java
        |           |-- GameFrame.java
        |           |-- GamePanel.java
        |           |-- GameState.java
        |           |-- GameMap.java
        |           |-- Player.java
        |           |-- Ghost.java
        |           |-- Direction.java
        |           |-- TileType.java
        |           `-- AssetLoader.java
        `-- resources/
            `-- assets/
                |-- player_avatar.png | .jpg | .jpeg
                |-- player_full.png | .jpg | .jpeg
                `-- fonts/
                    `-- arcade.ttf
```

## Risorse opzionali

Metti le immagini in `src/main/resources/assets/`. Sono supportati `.png`, `.jpg` e `.jpeg`:

- `player_avatar.png`, `player_avatar.jpg` o `player_avatar.jpeg`: volto/avatar del giocatore durante la partita.
- `player_full.png`, `player_full.jpg` o `player_full.jpeg`: immagine completa mostrata nella schermata iniziale.

Per l'avatar in partita il gioco cerca prima `player_avatar` e poi `player_full`. Per il menu cerca prima `player_full` e poi `player_avatar`.

Per il miglior risultato usa un avatar con sfondo trasparente. Se `player_avatar.jpg` ha uno sfondo bianco, PacSpanz prova a rendere trasparenti i pixel quasi bianchi dell'avatar in partita; questa pulizia non viene applicata all'immagine `player_full` del menu.

Puoi aggiungere un font arcade opzionale in:

```text
src/main/resources/assets/fonts/arcade.ttf
```

Se non viene trovato, il gioco usa un carattere monospaziato in grassetto.

## Comandi Maven

Compilazione e pacchetto:

```bash
mvn clean package
```

Avvio diretto:

```bash
mvn exec:java
```

## Controlli

- Frecce o WASD: movimento
- `P` o `ESC`: pausa/riprendi
- `M`: torna al menu dalla pausa
- `F11`: attiva/disattiva schermo intero
- `SPAZIO`: gioca dalla schermata iniziale
- `R`: ricomincia dopo sconfitta o vittoria

## Regole

- Pellet normale: +10 punti
- Pellet energia: +50 punti
- Fantasma vulnerabile raccolto: +200 punti
- Vite iniziali: 3
- Tunnel: attraversando un'apertura sul bordo, il giocatore e i fantasmi rientrano dal lato opposto del labirinto.
- Vittoria: tutti i pellet raccolti
- Sconfitta: vite esaurite
