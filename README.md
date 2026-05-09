# PacSpanz

PacSpanz è un minigioco arcade 2D a labirinto sviluppato in Java. Il giocatore si muove nella mappa, raccoglie pellet, può attivare pellet energia e deve evitare i fantasmi. La partita termina con la vittoria quando tutti i pellet sono stati raccolti, oppure con la sconfitta quando finiscono le vite.

Il progetto è pensato per un contesto universitario ed è realizzato senza librerie grafiche esterne.

## Tecnologie usate

- Java 17
- Swing e AWT per interfaccia, input e rendering 2D
- Maven per compilazione, packaging ed esecuzione
- Risorse statiche in `src/main/resources`

## Requisiti

- JDK 17 o superiore
- Maven 3.x
- Sistema operativo con supporto a Java Desktop/Swing

## Comandi Maven

Compilare il progetto:

```bash
mvn clean compile
```

Creare il pacchetto JAR:

```bash
mvn clean package
```

Eseguire il JAR generato:

```bash
java -jar target/pacspanz-1.0.0.jar
```

Eseguire il gioco tramite Maven:

```bash
mvn exec:java
```

## Comandi di gioco

- `SPAZIO`: avvia la partita dalla schermata iniziale
- Frecce direzionali o `WASD`: muovi il giocatore
- `P` o `ESC`: pausa o riprendi la partita
- `M`: torna al menu dalla schermata di pausa
- `R`: ricomincia dopo vittoria o sconfitta
- `F11`: attiva o disattiva lo schermo intero

## Struttura del progetto

```text
PacSpanz/
|-- pom.xml
|-- README.md
`-- src/
    `-- main/
        |-- java/
        |   `-- it/pacspanz/
        |       |-- Main.java
        |       |-- GameFrame.java
        |       |-- GamePanel.java
        |       |-- GameState.java
        |       |-- GameMap.java
        |       |-- Player.java
        |       |-- Ghost.java
        |       |-- Direction.java
        |       |-- TileType.java
        |       `-- AssetLoader.java
        `-- resources/
            `-- assets/
                |-- player_avatar.jpg
                |-- player_full.jpg
                `-- fonts/
                    `-- arcade.ttf
```

## Classi principali

- `Main`: punto di ingresso dell'applicazione.
- `GameFrame`: finestra principale del gioco e gestione dello schermo intero.
- `GamePanel`: pannello principale; gestisce ciclo di gioco, input, stato della partita e rendering.
- `GameMap`: rappresenta la mappa, i pellet, i tunnel e gli spawn di giocatore e fantasmi.
- `Player`: posizione, direzione e movimento del giocatore.
- `Ghost`: posizione, movimento e comportamento dei fantasmi.
- `GameState`: stati principali del gioco, come menu, partita, pausa, vittoria e sconfitta.
- `Direction`: direzioni di movimento usate da giocatore e fantasmi.
- `TileType`: tipi di celle presenti nella mappa.
- `AssetLoader`: caricamento di immagini e font dalle risorse del progetto.

## Asset e font

Gli asset usati dal gioco si trovano in `src/main/resources/assets`.

- `player_avatar.jpg`: immagine usata per il giocatore durante la partita.
- `player_full.jpg`: immagine mostrata nella schermata iniziale.
- `fonts/arcade.ttf`: font arcade caricato dall'applicazione.

Il codice supporta anche varianti `.png`, `.jpg` e `.jpeg` per le immagini del giocatore, cercandole in ordine di priorità. Se il font `fonts/arcade.ttf` non viene trovato, il gioco usa un font monospaziato di fallback.

## Regole sintetiche

- Pellet normale: +10 punti
- Pellet energia: +50 punti
- Fantasma vulnerabile: +200 punti
- Vite iniziali: 3
- I tunnel permettono a giocatore e fantasmi di rientrare dal lato opposto della mappa.
