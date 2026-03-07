/************************************************************
Versuch: 1-1
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"			// Praktikumsspezifische Definitionen
.include "lib_uart.inc"			// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text							// hier beginnt ein Code-Segment
.align	2						// Ausrichtung an eine gerade Adresse
.global	main					// "main" wird als globales Symbol deklariert
.syntax unified

// Konstantendeklarationen
.equ LF, 0x0A
.equ MASK, 0x1F					// Maske fuer die unteren fuenf Bit
.equ INITWERT, 0xFFFF			// initialer Wert für die Variablen

.equ RFSYS_REG0, 0x40041000
.equ RFSYS_REG4, 0x40041010

// Bit band Alias Adresse des Registers
// Für die nachfolgenden Versuche sollten Sie dann das zur Verfuegung gestellte Makro verwenden (s. k1514.inc)
.equ BB_RFSYS_REG4, 0x42000000 + (RFSYS_REG4 - 0x40000000) << 5 

/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind.
*/
main:
// Initialisiere UART 
	bl	uart_init

    LDM r8,{r0,r2,r9}
    STMDB r1!,{r3-r6,r11,r12}^

	ldr r6, =RFSYS_REG0			// mehrfach benoetigte Adressen in hohe Register vorladen
	ldr r7, =RFSYS_REG4
	ldr r8, =BB_RFSYS_REG4

// Da Register nicht im Datenbereich liegen, muessen diese zur Laufzeit initialisiert werden. 
	ldr r0, =INITWERT
	str r0, [r6]
	str r0, [r7]
	
	mov r10, #0					// eine Null und 
	mov	r11, #1					// eine Eins benoetigt man immer mal wieder
	
loop_main:
	// Ausgabe des initialen Wertes
	ldr r0, =str_init
	bl uart_putString
	ldr r0, [r6]				// Inhalt von RFSYS_REG0 laden,
	mov r9, r0					// wegen des UP-Aufrufs sichern
	bl uart_putInt32			// und ausgeben

	// Einlesen der Position des 0-Bits
	ldr	r0, =str0				// Eingabeaufforderung
	bl	uart_putString			// ausgeben
	bl	uart_getByte			// Bit-Nr. einlesen
	and	r0, r0, #MASK			// Maskieren auf 5 Bits
	mov	r4, r0					// Sichern des maskierten Wertes
	bl	uart_putByte			// maskierten Wert ausgeben
	
	// Einlesen der Position des 1-Bits
	ldr	r0, =str1
	bl	uart_putString
	bl	uart_getByte
	and	r0, r0, #MASK			
	mov	r5, r0					
	bl	uart_putByte

	// Bit-Manipulation ohne Bit Banding
	ldr r0, =str_mod_o			// Textausgabe
	bl uart_putString
	lsl	r2, r11, r4 			// Eine #1 an die gewuenschte Position (R4) schieben; in R11 ist eine #1
	bic r1, r9, r2				// Loeschen, 0-Bit-Position
	lsl r2, r11, r5
	orr	r0, r1, r2				// Setzen, 1-Bit-Position
	str r0, [r6]				// zurueckschreiben
	bl uart_putInt32
	
	// Bit-Manipulation mit Bit Banding
	// Position (in R4/R5) muss mit 4 multipliziert werden (BB je 32 Bit); Basis in R8 (s. o.)
	str	r10, [r8, r4, lsl #2]	// Selektiertes Bit (BB-Alias in R1) zuruecksetzen; in R10 ist eine #0 abgelegt
	str	r11, [r8, r5, lsl #2]	// Selektiertes Bit (BB-Alias in R1) setzen; R11 = #1 (s. o.)
		
	ldr r0, =str_mod_m
	bl uart_putString
	ldr r0, [r7]				// Inhalt des Registers RFSYS_REG4 laden
	bl uart_putInt32			// und ausgeben

	b loop_main					// zurueck zur Hauptschleife

/*** Datenbereich (ab 0x20000000) ***/
.data
str0: .asciz "\n\nNummer des 0-Bits: 0x"
str1: .asciz "\nNummer des 1-Bits: 0x"
str_init: .asciz "\n\nInitialer Wert: 0x"
str_mod_o: .asciz  "\n\nRFSYS_REG0: 0x"
str_mod_m: .asciz  "\nRFSYS_REG4: 0x"

/************************************************************
Versuch: 1-2
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"			// Praktikumsspezifische Definitionen
.include "lib_pit.inc"			// Einfache Unterprogramme zur Zeitmessung
.include "lib_uart.inc"			// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text							// hier beginnt ein Code-Segment
.align	2						// Ausrichtung an eine gerade Adresse
.global	main					// "main" wird als globales Symbol deklariert
.syntax unified

// Konstantendeklarationen
.equ	LF, 0x0A
.equ	P3_OH, 0x0B				// vorab ermittelter Overhead fuer pit3_getval
	
/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind.
*/
main:
	bl	pit3_init			// Initialisiere PIT3
	bl	uart_init			// Initialisiere UART 

	ldr	r10, =vec1			// Zeiger auf ersten Vektor
	ldr	r11, =vec2			// zeiger auf zweiten Vektor
	
main_loop:
	ldr r0, =str_eingabe
	bl	uart_putString
	
// Vektor 1 einlesen, abspeichern und ausgeben
	ldr r0, =str_vek1		
	bl	read_vector
	str	r0, [r10]	
	bl	uart_putInt32
	
// Vektor 2 einlesen, abspeichern und ausgeben
	ldr r0, =str_vek2
	bl	read_vector
	str r0, [r11]
	bl	uart_putInt32
	
// Min- und Max-Vektor berechnen und in R8 und R9 speichern
	mov r3, #4				// 4 Durchläufe
	ldr r8, [r10]			// Min-Vektor mit erstem Vektor vorbelegen
	ldr r9, [r11]			// Max-Vektor mit zweitem Vektor vorbelegen

// *** Anfang des Bereichs für die Messung der Dauer ***	
	bl pit3_getval			// ersten PIT3-Zaehlerwert holen
	mov r12, r0				// in R12 sichern

calc_min_max:
	ror r8, r8, #24			// um 24 Bit rechts rotieren
	ror r9, r9, #24			// um 24 Bit rechts rotieren
	ubfx r0, r8, #0, #8		// Byte vom erster Vektor extrahieren
	ubfx r1, r9, #0, #8		// Byte vom zweiten vektor extrahieren
	cmp r0, r1				// R0 > R1?
	itt hi
	bfihi r9, r0, #0, #8	// Entsprechendes Byte im Max-Vektor ersetzen
	bfihi r8, r1, #0, #8	// Entsprechendes Byte im Min-Vektor ersetzen
	subs r3, r3, #1			// Schleifenzähler dekrementieren
	bne calc_min_max		// 4x durchlaufen? nein => nochmal

// *** Ende des Bereichs für die Messung der Dauer ***
	bl	pit3_getval			// zweiten PIT3-Zaehlerwert holen	
	sub r12, r12, r0		// Differenz zum alten Timerwert (PIT zaehlt runter)
   
// Textausgabe Teil b)		
	ldr r0, =str_ohne_dsp
	bl	uart_putString
	
// Minimum ausgeben
	mov r0, r8
	bl uart_putInt32
	
// Maximum ausgeben
	ldr r0, =str_max
	bl	uart_putString
	mov r0, r9
	bl uart_putInt32
	
// Dauer für Teil b) ausgeben
	ldr r0, =str_dauer
	bl uart_putString
	sub	r0, r12, #P3_OH		// Overhead für pit3_getval subtrahieren und Ergebnis für die Ausgabe vorbereiten
	bl uart_putByteBase10
	ldr	r0, =str_takte
	bl	uart_putString

	ldr r4, [r10]			// ersten Vektor laden
	ldr r5, [r11]			// zweiten Vektor laden

// *** Anfang des Bereichs für die Messung der Dauer ***
	bl pit3_getval			// ersten PIT3-Zaehlerwert holen
	mov r12, r0				// in R12 sichern
	
// Min- und Max-Vektor mit DSP-Instruktionen berechnen
	usub8 r2, r4, r5
	sel r6, r5, r4			// Min-Vektor in R6
	sel r7, r4, r5			// Max-Vektor in R7
// *** Ende des Bereichs für die Messung der Dauer ***

	bl	pit3_getval			// zweiten PIT3-Zaehlerwert holen
	sub r12, r12, r0		// Differenz zum alten Timerwert (PIT zaehlt runter)
	
// Textausgabe Teil c)
	ldr r0, =str_mit_dsp
	bl	uart_putString
	
	mov r0, r6				// Min-Vektor nach R0
	bl uart_putInt32
		
	ldr r0, =str_max
	bl	uart_putString
	mov r0, r7				// Max-Vektor nach R0
	bl uart_putInt32
	
// Dauer für Teil c) ausgeben
	ldr r0, =str_dauer
	bl uart_putString
	sub	r0, r12, #P3_OH		// Overhead für pit3_getval subtrahieren und Ergebnis für die Ausgabe vorbereiten
	bl uart_putByteBase10
	ldr	r0, =str_takte
	bl	uart_putString

// Rücksprung zur Hauptschleife	
	b main_loop 
	
/*** Unterprogramm ***/	

/*
 * Liest vier Byte ein und fuegt diese zu einem 32-bit-Vektor zusammen
 * Parameter: R0: Zeiger auf auszugebenden string
 * Rueckgabe: R0: Eingelesener 32-bit-Vektor
 */		
.thumb_func
read_vector:
	push {r4,r5,LR}
	bl	uart_putString
	mov r4, #0				// Ergebnisregister initialisieren (wg. ORR)
	mov r5, #0x4			// Zaehler initialisieren	
read_byte:
	bl 	uart_getByte		// ein Byte vom Terminal einlesen
	orr r4, r0, r4, lsl #8	// Ergebnisregister R4 vorbereiten und R0 ins Ergebnisregister uebertragen
	subs r5, r5, #1			// Zaehler dekrementieren
	bne read_byte			// Zaehler = 0? nein => nochmal
	mov r0, r4				// Rueckgabewert eintragen
	pop {r4,r5,PC}

/*** Datenbereich (ab 0x20000000) ***/
.data
vec1: .word 0
vec2: .word 0
str_eingabe: .asciz "\nGeben Sie die beiden Vektoren ein."
str_ohne_dsp: .asciz "\n\nOhne DSP-Instruktionen\nmin: 0x"
str_mit_dsp: .asciz "\nMit DSP-Instruktionen\nmin: 0x"
str_max: .asciz "\nmax: 0x"
str_vek1: .asciz "\nv1: 0x"
str_vek2: .asciz "\nv2: 0x"
str_dauer: .asciz "\nDauer: "
str_takte: .asciz " Takte\n"

/************************************************************
Versuch: 1-3
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"			// Praktikumsspezifische Definitionen
.include "lib_uart.inc"			// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text							// hier beginnt ein Code-Segment
.align	2						// Ausrichtung an eine gerade Adresse
.global	main					// "main" wird als globales Symbol deklariert
.syntax unified

// Konstantendeklarationen
.equ INNER, 4
.equ OUTER, 4

.equ KA, 0xA4367C7B
.equ KB, 0x10A78933
.equ KC, 0xC5535F2C
.equ KD, 0x788E84B1

.equ SAT13, 13

.equ LF, 0x0A
.equ SPACE, 0x20

/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind.
*/
main:
// Initialisiere UART 
	bl	uart_init
	
// *** Teil a) ***
	ldr	r8, =matrix_m		// Anfangsadresse Matrix M
	ldr r10, =matrix_o		// Ausgabematrix

  	mov r2, #OUTER			// Zaehler aeussere Schleife
outerloop:	
  	ldm r8!, {r4,r5}		// Zeile aus M lesen
	ldr r9, =matrix_n		// Anfangsadresse Matrix N
	
  	mov r3, #INNER			// Zaehler innere Schleife
innerloop:
  	ldm r9!, {r6,r7}		// Spalte (=Zeile aus transponierter) Matrix N
	
  	smuad r0, r4, r6		// Die ersten beiden Eintraege miteinander multiplizieren
  	smlad r0, r5, r7, r0	// Die beiden naechsten Eintraege miteinander
							// multiplizieren und auf vorheriges Ergebnis akkumulieren
	
  	str r0, [r10], #4		// Ergebnis speichern
	
  	subs r3, r3, #1			// Z-Flag wird gesetzt, sobald 0 erreicht wird
  	bne innerloop			// NE prueft Z-Flag, wenn dies 1 ist, ist 0
							// erreicht, ansonsten (NE) wird die Schleife wiederholt

  	subs r2, r2, #1			// Z-Flag wird gesetzt, sobald 0 erreicht wird
  	bne outerloop			// NE prueft Z-Flag, wenn dies 1 ist, ist 0
							// erreicht, ansonsten (NE) wird die Schleife wiederholt

// Ausgabe der Ergebnismatrix
	ldr	r0, =str_ergebnis
	bl	uart_putString

	ldr	r4, =matrix_o		// Start der Ergebnismatrix
	mov	r5, #0				// Schleifenzaehler und Index
output_loop:
	ldr	r0, [r4, r5, lsl #2]// Wort laden, Index inkrementieren, wortweise zugreifen	
	bl	uart_putInt32
	and r6, r5, #3			// Zaehler maskieren
	cmp r6, #3				// bei einem Stand von #3 wurde eine Zeile ausgegeben
	ite eq
	moveq r0, #LF			// Linefeed
	movne r0, #SPACE		// oder Space
	bl uart_putChar			// ausgeben
	add	r5, r5, #1			// Zaehler inkrementieren
	cmp r5, #(OUTER*INNER)	// alle Werte ausgegeben?
	bne output_loop			// nein => naechste Zeile ausgeben
	
// *** Teil b)  ***
	ldr	r0, =str_copy		// Text zu Teil b) ausgeben
	bl	uart_putString
	ldr	r0, =matrix_o		// Zeiger auf Quelle (O)
	ldr r1, =matrix_o2		// Zeiger auf Ziel (O2)
	ldm	r0!, {r4-r11}		// acht 32-bit-Werte laden; mit Auto-Inkrement der Quelladresse
	stm	r1!, {r4-r11}		// acht 32-bit-Werte speichern; mit Auto-Inkrement der Zieladresse
	ldm	r0, {r4-r11}		// das Ganze zwei Mal, dann ist alles kopiert
	stm	r1, {r4-r11}		// (beim 2. Mal ohne Auto-Inkrement)
	
// *** Teil c) ***
	ldr	r0, =str_prodsum	// Text zu Teil c) ausgeben
	bl	uart_putString
	
	mov r1, #OUTER			// Zaehler,
	mov r0, #0				// Akkumulator
	ldr r5, =matrix_m		// und Zeiger initialisieren
	ldr r6, =matrix_n
loop_spur:
	ldm r5!, {r7, r9}		// vier 16-bit-Werte aus M holen, Adresszeiger inkrementieren
	ldm r6!, {r8, r10}		// vier 16-bit-Werte aus N holen, Adresszeiger inkrementieren
	smlad r0, r7, r8, r0	// zwei mal zwei 16-bit-Werte multiplizieren und akkumulieren 
	smlad r0, r9, r10, r0	// zwei mal zwei 16-bit-Werte multiplizieren und akkumulieren
	subs r1, r1, #1			// Zaehler dekrementieren
	bne loop_spur			// vier Mal durchlaufen? nein => nochmal
	bl	uart_putInt32		// und ausgeben
	

// *** Teil d) ***
	ldr	r4, =KA				// Konstanten fuer die Berechnungen laden
	ldr	r5, =KB
	ldr	r6, =KC
	ldr	r7, =KD
	
// Ausgabe "\nUADD16: "
	ldr r0, =str_uadd
	bl uart_putString
	
// Berechnungen mit UADD16 und Ausgabe der Ergebnisse
	uadd16 r0, r4, r5
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	uadd16 r0, r4, r6
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	uadd16 r0, r4, r7
	bl uart_putInt32
	
// Ausgabe "\nSADD16: "
	ldr r0, =str_sadd
	bl uart_putString
	
// Berechnungen mit SADD16 und Ausgabe der Ergebnisse
	sadd16 r0, r4, r5
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	sadd16 r0, r4, r6
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	sadd16 r0, r4, r7
	bl uart_putInt32
	
// Ausgabe "\nUQADD16: "
	ldr r0, =str_uqadd
	bl uart_putString
	
// Berechnungen mit UQADD16 und Ausgabe der Ergebnisse
	uqadd16 r0, r4, r5
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	uqadd16 r0, r4, r6
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	uqadd16 r0, r4, r7
	bl uart_putInt32
	
// ausgabe "\nQADD16: "
	ldr r0, =str_qadd
	bl uart_putString
	
// Berechnungen mit QADD16 und Ausgabe der Ergebnisse
	qadd16 r0, r4, r5
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	qadd16 r0, r4, r6
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	qadd16 r0, r4, r7
	bl uart_putInt32
		
// *** Teil e) ***
// R4, R5 enthalten noch die Konstanten A und B
// Ausgabe "\nSSAT16: "
	ldr r0, =str_ssat
	bl uart_putString

	SSAT16 r7, #12, r7
    USAT16 r0, #7, r5

// Berechnungen mit SSAT16 und Ausgabe der Ergebnisse
	ssat16 r0, #SAT13, r4
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	ssat16 r0, #SAT13, r5
	bl uart_putInt32
	
// Ausgabe "\nUSAT16: "
	ldr r0, =str_usat
	bl uart_putString
	
// Berechnungen mit USAT16 und Ausgabe der Ergebnisse
	usat16 r0, #SAT13, r4
	bl uart_putInt32
	mov r0, #SPACE
	bl uart_putChar
	usat16 r0, #SAT13, r5
	bl uart_putInt32
	
end:
	b end			// Endlosschleife, damit das Programm nicht ins "Nirwana" laeuft.
	
/*** Datenbereich (ab 0x20000000) ***/
.data
// Matrix M
matrix_m: .hword 0x5f8c, 0x7f48, 0x8245, 0x6048, 0xc799, 0x9c5d, 0xd49c, 0x33d8, 0x579b, 0x8aef, 0xf2c0, 0xa3c2, 0x41fd, 0x50d4, 0x25c6, 0x4afa
// Matrix N (transponiert abgelegt)
matrix_n: .hword 0x7f41, 0x0645, 0x8c8f, 0x9065, 0x78e3, 0xf7f3, 0xd29f, 0x2961, 0x5a1e, 0xdfed, 0x4b9f, 0x1092, 0x61bf, 0x21be, 0x5ca1, 0xc6d3
// Platz fuer die Ergebnismatrix
matrix_o: .rept OUTER * INNER
	.word 0
.endr
// Platz fuer die Kopie der Ergebnismatrix
matrix_o2: .rept OUTER * INNER
	.word 0
.endr
str_ergebnis: .asciz "Ergebnismatrix O: \n"
str_prodsum: .asciz "\nProdukt-Summe: "
str_copy: .asciz "\nKopieren der Matrix O!\n"
str_uadd: .asciz "\n\nUADD16:  "
str_sadd: .asciz "\nSADD16:  "
str_uqadd: .asciz "\nUQADD16: "
str_qadd: .asciz "\nQADD16:  "
str_ssat: .asciz "\n\nSSAT16:  "
str_usat: .asciz "\nUSAT16:  "

/************************************************************
Versuch: 2-1
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"				// Praktikumsspezifische Definitionen
.include "lib_uart.inc"				// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text								// hier beginnt ein Code-Segment
.align	2							// Ausrichtung an eine gerade Adresse
.global	main						// "main" wird als globales Symbol deklariert
.syntax unified

// Registeradressen
.equ SIM_SCGC6,	 	0x4004803C
.equ SCR_VTOR,		0xE000ED08		// Register für die Interrupt-Vektor-Tabelle (IVT)

.equ PIT_MCR,       0x40037000
.equ PIT_LDVAL1,    0x40037110
.equ PIT_TCTRL1,    0x40037118
.equ PIT_TFLG1,     0x4003711C

.equ NVIC_ISER2, 	0xe000e108
.equ NVIC_ICPR2, 	0xe000e288

// Bit Band-Konstanten (s. k1514.inc)
__BBREG BB_SIM_PIT, SIM_SCGC6, 23	// Bit-Band-Alias-Adresse für den PIT im SIM-Register

// weitere Konstanten
.equ PIT1_IRQ_OFS,	0x154			// Interrupt Request Offset für PIT1
.equ PIT1_NVIC_MASK, 1 << (69 % 32)	// Bitposition für PIT-IRQ in NVIC-Registern (IRQ mod 32)

.equ PIT_COUNT, 25000000-1			// 25000000	Takte bei 25 MHz => 1 Sekunde
.equ PIT_MCR_VAL, 0					// MDIS = 0, FRZ = 0
.equ PIT_TCTRL1_VAL, 3				// CHN = 0, TIE = 1, TEN = 1
  
.equ LF, 0x0A
.equ SPACE, ' '
.equ CHAR_S, 's'				
.equ CHAR_T, 't'
.equ CHAR_D, 'd'
.equ DOT, '.'
.equ COLON, ':'
.equ MAX_MI, 60						// Maxima für Zeit und Datum
.equ MAX_H, 24
.equ MAX_D, 30
.equ MAX_MO, 12
.equ MAX_Y, 100
.equ CENTURY, 20					// Jahrtausend

.equ O_STD, 0						// Array-Offsets
.equ O_MIN, 1
.equ O_TAG, 2
.equ O_MON, 3
.equ O_JHR, 4
	
.equ BFL, 8							// Bitfeld-Laenge
.equ BFP_STD, 8*O_STD				// Bitfeld-Positionen
.equ BFP_MIN, 8*O_MIN
.equ BFP_TAG, 8*O_TAG
.equ BFP_MON, 8*O_MON

/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind.
*/
main:
// Initialisiere UART 
	bl uart_init
	
// PIT1 initialisieren
	bl pit1_init
	
	mov r11, #1						// Start-/Stopp-Flag (1: laeuft, 0: angehalten)
	ldr r10, =date_time				// Zeiger auf das Zeit-/Datum-Array
	mov r9, #0						// Defaultwert für das Benutzerflag
	ldr r8, =pit_flag				// Adresse des Benutzerflags laden
	
mainloop:
	bl	uart_charPresent			// wurde ein Zeichen eingegeben?
	cbnz r0, check_input			// ja, pruefen
	cmp r11, #1						// laeuft die Uhr?
	bne	mainloop					// nein, nur die Eingabe abfragen
	
	ldrb r0, [r8]					// Flag laden
	cmp	r0, #1						// PIT-Flag = Eins? 
	bne	mainloop					// Nein -> Hauptschleife
	strb r9, [r8]					// PIT-Flag zuruecksetzen
	
	bl	inc_datetime				// die Uhr weiterschalten
	bl	put_datetime				// Datum und Zeit ausgeben
	b 	mainloop					// das Ganze von vorn

check_input:
	bl	uart_getChar				// eingegebenes Zeichen einlesen
	cmp	r0, #CHAR_S					// war es ein 's'?
	beq	ci_s						// ja, verzeigen
	cmp r0, #CHAR_T					// war es ein 't'?
	beq ci_t						// ja, verzeigen
	cmp r0, #CHAR_D					// war es ein 'd'?
	beq ci_d						// ja, verzeigen
	b	mainloop					// keine gueltige Eingabe, normal weitermachen
	
ci_s:
	eors r11, r11, #1				// Start/Stopp umkehren & pruefen
	ite ne							// laeuft?
	ldrne r0, =str_start			// entsprechenden String auswaehlen
	ldreq r0, =str_stopp
	bl	uart_putString				// gewaehlten String ausgeben
	b 	mainloop					// zurueck zum Anfang
	
ci_t:
	ldr r0, =str_std				// Text zur Stundenabfrage waehlen,
	bl	echo_input					// ausgeben und Eingabe abholen
	mov r4, r0						// sichern
	ldr r0, =str_min				// gleicher Vorgang fuer die Minuten
	bl	echo_input
	orr r1, r4, r0, lsl #8			// Stunden & Minuten zusammenbauen
	strh r1, [r10, #O_STD]			// und speichern
	b 	mainloop					// zurueck zum Anfang
	
ci_d:
	ldr r0, =str_tag				// Text zur Tagesabfrage waehlen,
	bl	echo_input					// ausgeben und Eingabe abholen
	mov r4, r0						// sichern
	ldr r0, =str_mon				// gleicher Vorgang fuer den Monat 
	bl	echo_input
	orr r1, r4, r0, lsl #8			// Tag & Monat zusammenbauen
	strh r1, [r10, #O_TAG]			// und speichern
	ldr r0, =str_jhr				// gleicher Vorgang fuer das Jahr
	bl	echo_input
	strb r0, [r10, #O_JHR]
	b 	mainloop					// zurueck zum Anfang

/*** Unterprogramme und ISR ***/

/*
 * Gibt den Text (Zeiger in r0) aus und liest einen zweistelligen Dezimalwert ein, 
 * gibt ihn (zur Kontrolle) wieder aus, haengt einen Zeilenumbruch an
 * und gibt den Wert in r0 zurueck
 * Parameter: r0 = Zeiger auf Text
 * Rueckgabe: eingelesener Wert
 */		
.thumb_func
echo_input:
	push {r4,lr}
	bl	uart_putString				// uebergebenen String ausgeben
	bl	uart_getByteBase10			// einen zweistelligen Dezimalwert einlesen
	mov r4, r0						// sichern
	bl	out_leading0				// zur Kontrolle ausgeben
	mov	r0, #LF						// neue Zeile
	bl	uart_putChar				// ausgeben
	mov r0, r4						// gesicherten Wert zurueckgeben
	pop {r4,pc}

/*
 * Inkrementiert zunaechst nur die Minuten, bei einem Ueberlauf auch die jeweils naechsthoehere Einheit
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
inc_datetime:
	ldr r1, =date_time				// Zeiger auf das Array laden
	ldr r2, [r1]					// Stunden, Minuten, Tag & Monat laden
	mov r3, #0						// Reset-Wert (Stunden/Minuten)

	ubfx r0, r2, #BFP_MIN, #BFL		// Minuten extrahieren
	add	r0, r0, #1					// inkrementieren
	cmp	r0, #MAX_MI					// mit Maximum vergleichen
	itee hs							// Maximum erreicht?
	bfihs r2, r3, #BFP_MIN, #BFL	// Reset-Wert oder
	bfilo r2, r0, #BFP_MIN, #BFL	// aktuellen Wert einfügen
	strlo r2, [r1] 					// Nur speichern, wenn kein Ueberlauf 
	blo inc_datetime_end			// Sprung zum Ende, wenn kein Ueberlauf

	ubfx r0, r2, #BFP_STD, #BFL		// Stunden extrahieren
	add	r0, r0, #1					
	cmp	r0, #MAX_H	
	itee hs							// Maximum erreicht?
	bfihs r2, r3, #BFP_STD, #BFL
	bfilo r2, r0, #BFP_STD, #BFL
	strlo r2, [r1] 					// Nur speichern, wenn kein Ueberlauf 
	blo inc_datetime_end

	mov r3, #1						// Reset-Wert (Tag/Monat)
	ubfx r0, r2, #BFP_TAG, #BFL		// Tag extrahieren
	add	r0, r0, #1				
	cmp	r0, #MAX_D				
	itee hs							// Maximum erreicht?
	bfihs r2, r3, #BFP_TAG, #BFL
	bfilo r2, r0, #BFP_TAG, #BFL
	strlo r2, [r1]					// Nur speichern, wenn kein Ueberlauf 
	blo inc_datetime_end

	ubfx r0, r2, #BFP_MON, #BFL		// Monat extrahieren
	add	r0, r0, #1				
	cmp	r0, #MAX_MO				
	ite hs							// Maximum erreicht?
	bfihs r2, r3, #BFP_MON, #BFL
	bfilo r2, r0, #BFP_MON, #BFL
	str r2, [r1]					// Spaetestens hier muss gespeichert werden 
	blo inc_datetime_end

	ldrb r0, [r1, #O_JHR]			// Jahr laden
	add	r0, r0, #1	
	cmp	r0, #MAX_Y	
	it hs			
	movhs r0, #0	 
	strb r0, [r1, #O_JHR]

inc_datetime_end:
	bx lr
	
/*
 * Gibt Datum und Zeit auf dem Terminal aus
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
put_datetime:
	push {r4,r5,lr}
	ldr	r4, =date_time				// Zeiger auf das Array laden
	ldr r5, [r4]					// Stunden, Minuten, Tag & Monat laden
	ubfx r0, r5, #BFP_TAG, #BFL		// Tag extrahieren
	bl	out_leading0
	mov	r0, #DOT					// Punkt
	bl	uart_putChar
	ubfx r0, r5, #BFP_MON, #BFL		// Monat extrahieren
	bl	out_leading0
	mov	r0, #DOT					// Punkt
	bl	uart_putChar
	mov	r0, #CENTURY				// Jahrtausend 
	bl	uart_putByteBase10
	ldrb r0, [r4, #O_JHR]			// Jahr (kurz)
	bl	out_leading0
	mov	r0, #SPACE					// Leerzeichen
	bl	uart_putChar
	ubfx r0, r5, #BFP_STD, #BFL		// Stunde extrahieren
	bl	out_leading0
	mov	r0, #COLON					// Doppelpunkt
	bl	uart_putChar
	ubfx r0, r5, #BFP_MIN, #BFL		// Minute extrahieren
	bl	out_leading0
	ldr r0, =str_uhr
	bl uart_putString
	pop	{r4,r5,pc}

/*
 * Gibt bei Zahlen < 10 eine fuehrende Null aus
 * Parameter: 8-bit-Zahl in R0
 * Rueckgabe: keine
 */		
.thumb_func
out_leading0:
	push {lr}
	cmp r0, #10						// >= 10? 
	ite hs
	ldrhs r1, =uart_putByteBase10	// ohne fuehrende Null
	ldrlo r1, =uart_putByte			// mit fuehrender Null
	blx r1							// entsprechendes UP aufrufen
	pop {pc}

/*
 * Initialisiert den PIT
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
pit1_init:
	ldr r0, =SCR_VTOR				// Basisadresse der IVT
	ldr r1, [r0]					// laden
	ldr r0, =isr_pit1				// ISR fuer den PIT1 
	str r0, [r1, #PIT1_IRQ_OFS]		// in die IVT eintragen
	
	ldr r0, =NVIC_ICPR2				// Pending (haengende) IRQs zu PIT1 im NVIC loeschen
	mov r1, #PIT1_NVIC_MASK			// Bit für PIT1-IRQ in den NVIC-Registern
	str r1, [r0]
	ldr r0, =NVIC_ISER2				// IRQs zu PIT1 im NVIC aktivieren
	str r1, [r0]					// R1 hat noch den Wert (PIT1_NVIC_MASK), also nicht neu laden

	ldr r0, =BB_SIM_PIT				// Bit-Band-Alias-Adresse fuer das Bit des PIT im SIM laden
	mov r1, #1						
	str r1, [r0]					// Clock fuer PIT1 aktivieren
	ldr r0, =PIT_MCR				
	mov r1, #PIT_MCR_VAL			// alle PITs einschalten, im Debug-Modus anhalten
	str r1, [r0]
	ldr r0, =PIT_LDVAL1				// initialen Zaehlwert setzen
	ldr r1, =PIT_COUNT 
	str r1, [r0]
	ldr r0, =PIT_TCTRL1
	mov r1, #PIT_TCTRL1_VAL			// PIT1: aktivieren, IRQs aktiv 
	str r1, [r0]
 	bx lr

/*
 * PIT-ISR (Interrupt Service Routine)
 * Parameter: keine Parameter moeglich, da die ISR asynchron zum Programmablauf aufgerufen wird	
 * Rueckgabe: keine Rueckgabe moeglich, da die ISR asynchron zum Programmablauf aufgerufen wird	
 */		
.thumb_func
isr_pit1:
	mov r1, #0x1
	ldr r0, =pit_flag				// Benutzerflag setzen
	str r1, [r0]
	ldr r0, =PIT_TFLG1				// TIF (Timer Interrupt Flag) loeschen
	str r1, [r0]
	bx lr
	
/*** Datenbereich (ab 0x20000000) ***/
.data
pit_flag: .word 0
date_time: .byte 0,0,1,1,14		// Stunde, Minute, Tag, Monat, Jahr (vorinitialisiert)
str_std: .asciz "Stunde eingeben (hh): "
str_min: .asciz "Minute eingeben (mm): "
str_tag: .asciz "Tag eingeben (dd): "
str_mon: .asciz "Monat eingeben (mm): "
str_jhr: .asciz "Jahr eingeben (jj): "
str_start: .asciz "Uhr gestartet\n"
str_stopp: .asciz "Uhr angehalten\n"
str_uhr: .asciz " Uhr\n"

/************************************************************
Versuch: 2-2
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"			// Praktikumsspezifische Definitionen 
.include "lib_pit.inc"			// Einfache Unterprogramme zur Zeitmessung
.include "lib_uart.inc"			// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text							// hier beginnt ein Code-Segment
.align	2						// Ausrichtung an eine gerade Adresse
.global	main					// "main" wird als globales Symbol deklariert
.syntax unified

// Registeradressen
.equ SIM_SCGC5,     0x40048038
.equ SIM_SCGC6,     0x4004803C

.equ GPIOA_PDOR,    0x400FF000
.equ GPIOA_PDIR,    0x400FF010
.equ GPIOA_PDDR,    0x400FF014

.equ GPIOE_PDIR,    0x400FF110
.equ GPIOE_PDDR,    0x400FF114

.equ PORTA_PCR0,	0x40049000	// Start-Adresse der Port A PCR-Register

/* Bei alternativer Adressierung
.equ PORTA_PCR10,   0x40049028
.equ PORTA_PCR11,   0x4004902c
.equ PORTA_PCR19,   0x4004904C
.equ PORTA_PCR28,   0x40049070
.equ PORTA_PCR29,   0x40049074
*/

.equ PORTE_PCR26,   0x4004d068

.equ PIT_MCR,       0x40037000
.equ PIT_TCTRL0,    0x40037108
.equ PIT_TCTRL1,    0x40037118

.equ PIT_LDVAL0,    0x40037100
.equ PIT_TFLG0,     0x4003710C
.equ PIT_LDVAL1,    0x40037110
.equ PIT_TFLG1,     0x4003711C

.equ NVIC_ISER2,	0xE000E108
.equ NVIC_ICPR2, 	0xE000E288
.equ SCR_VTOR,		0xE000ED08

// Bit Band-Konstanten
__BBREG	SIM_PORTA, SIM_SCGC5, 9
__BBREG	SIM_PORTE, SIM_SCGC5, 13
__BBREG	SIM_PIT, SIM_SCGC6, 23

__BBREG	PDDR_SW1, GPIOA_PDDR, 19
__BBREG	PDDR_SW2, GPIOE_PDDR, 26

__BBREG	SW1, GPIOA_PDIR, 19
__BBREG	SW2, GPIOE_PDIR, 26
__BBREG	E1, GPIOA_PDOR, 11
__BBREG	E2, GPIOA_PDOR, 28
__BBREG	E3, GPIOA_PDOR, 29
__BBREG	E4, GPIOA_PDOR, 10

__BBREG	SW2_CIF, PORTE_PCR26, 24

__BBREG	PIT_FRZ, PIT_MCR, 0
__BBREG	PIT_MDIS, PIT_MCR, 1
__BBREG	PIT0_TEN, PIT_TCTRL0, 0
__BBREG	PIT1_TEN, PIT_TCTRL1, 0
__BBREG	PIT1_TIE, PIT_TCTRL1, 1

// weitere Konstanten
.equ ONE_SECOND, 25000000-1		// 25.000.000 Takte bei 25 MHz
.equ FIVE_MS, 125000-1			// 125.000 Takte bei 25 MHz 
.equ LF, 0x0A					// Line Feed (neue Zeile)
.equ CHAR_S, 's'
.equ CHAR_PL, '+'
.equ CHAR_MI, '-'

.equ PDDR_MASK_LED, (1<<10 | 1<<11 | 1<<28 | 1<<29)	// Maske fuer die LEDs E1 bis E4

.equ GPIO_SET, 0x120			// GPIO-Modus, Open-Drain
.equ GPIO_PE_SET, 0x103			// GPIO-Modus, Pull-Up-Widerstand
.equ GPIO_IRQ_PE_SET, 0xA0103	// GPIO-Modus, IRQ (fallende Flanke), Pull-Up-Widerstand

.equ PIT0_INIT, 2				// Interrupt aktiviert, Timer nicht aktiviert
.equ PIT1_INIT, 3				// nicht mit PIT0 verkettet, Interrupt aktiviert, Timer aktiviert

// IVT-Offsets
.equ IVT_PIT0, 0x150			
.equ IVT_PIT1, 0x154
.equ IVT_PTE, 0x1AC

.equ NVIC_ISER2_MASK, 0x8000030 // Bit 27 (Port E), 5 (PIT1), 4 (PIT0)

// Offsets für die einzelnen PCR-Register
// Alternativ ist auch (Portnummer<<2) möglich
.equ PCR10,	0x28				// E4
.equ PCR11,	0x2C				// E1
.equ PCR19,	0x4c				// SW1
.equ PCR28,	0x70				// E2
.equ PCR29,	0x74				// E3

/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind.
*/
main:	
// Initialisiere UART (Terminal) 
	bl uart_init
	
// Fuer die Initialisierung haeufig verwendete Werte vorbelegen
	mov r10, #0
	mov r11, #1 

// Takt fuer die benutzten Module aktivieren
	ldr r0, =SIM_PORTA
	str r11, [r0]
	ldr r0, =SIM_PORTE
	str r11, [r0]
	ldr r0, =SIM_PIT
	str r11, [r0]
	
// GPIOE-Pin konfigurieren
	ldr r1, =GPIO_IRQ_PE_SET 	// GPIO-Modus mit IRQ (fallende Flanke) und Pull-Up-Widerstand 	
	ldr r0, =PORTE_PCR26 		// fuer SW2
	str r1, [r0]				// festlegen
	
// Da die Lage der Konfigurations-Register nicht aenderbar ist, sind auch alternative Adressierungen moeglich.
/*	
 * Adressierung mit Offset (bevorzugt):
 */
	ldr r0, =PORTA_PCR0 		// Startadresse der Port A PCR-Register

	ldr r1, =GPIO_PE_SET 		// GPIO-Modus mit Pull-Up-Widerstand
	str r1, [r0, #PCR19]		// für SW1 festlegen
 
	mov r1, #GPIO_SET 			// GPIO-Modus festlegen
 	str r1, [r0, #PCR11]		// E1
 	str r1, [r0, #PCR28]		// E2
 	str r1, [r0, #PCR29]		// E3
 	str r1, [r0, #PCR10]		// E4
 	
/*
 * Einzel-Adressierung der Register:
 * (Evtl. übersichtlicher) 
 *
	ldr r1, =GPIO_PE_SET 		// GPIO-Modus mit Pull-Up-Widerstand
	ldr r0, =PORTA_PCR19 		// fuer SW1
	str r1, [r0]				// festlegen

	mov r1, #GPIO_SET 			// GPIO-Modus festlegen
	ldr r0, =PORTA_PCR11 		// LED E1
	str r1, [r0]
	ldr r0, =PORTA_PCR28 		// LED E2
	str r1, [r0]
	ldr r0, =PORTA_PCR29 		// LED E3
	str r1, [r0]
	ldr r0, =PORTA_PCR10 		// LED E4
	str r1, [r0]

 *
 * Post-Increment:
 * (bietet sich an, wenn viele Register nacheinander beschrieben werden müssen)
 * (für die Konfiguration der LEDs allerdings eher ungeignet und nur der Vollstaendigkeit halber aufgefuehrt)
 *
	mov r1, #GPIO_SET 			// GPIO-Modus festlegen
	ldr r0, =PORTA_PCR10 		// LED E4
	str r1, [r0], #4			// eintragen und PCR11 adressieren
	str r1, [r0]				// LED E1
	ldr r0, =PORTA_PCR28 		// LED E2
	str r1, [r0], #4			// eintragen und PCR29 adressieren
	str r1, [r0]				// LED E3

 *
 * Nutzung von GPCLR/GPCHR:
 * (bei identischer Einstellung mehrerer Portbits)
 *
 * Nur sinnvoll nutzbar, wenn die vollständige Kontrolle des Systems nach dem Reset vorhanden ist!
 * 
 * Da dies im Fachpraktikum nicht der Fall ist, duerfen diese Register nicht zur Konfiguration verwendet werden!
 * (Das Feld "IRQC" kann mittels dieser Register nicht initialisiert werden!)
 *
*/	
	
// Datenrichtung der GPIO-Pins festlegen
// Eingaenge (SW1 und SW2)
	ldr r0, =PDDR_SW1
	str r10, [r0]
	ldr r0, =PDDR_SW2
	str r10, [r0]
	
// Ausgaenge (LEDs)
	ldr r0, =GPIOA_PDDR
	ldr r1, =PDDR_MASK_LED
	ldr r2, [r0]
	orr	r2, r2, r1
	str r2, [r0]
	
// Adressen der ISRs fuer PIT0, PIT1 und Port E in die IVT eintragen
	ldr r0, =SCR_VTOR
	ldr r1, [r0]
	ldr r0, =isr_port_e
	str r0, [r1, #IVT_PTE]
	ldr r0, =isr_pit0
	str r0, [r1, #IVT_PIT0]
	ldr r0, =isr_pit1
	str r0, [r1, #IVT_PIT1]

// PIT-Modul konfigurieren
	ldr r0, =PIT_MCR
	str r11, [r0]				// R11=1 -> FRZ=1, MDIS=0 (PIT stopped in Debug mode, PIT-Clock enabled)

// PIT0 auf fuenf Millisekunden einstellen
	ldr r0, =PIT_LDVAL0
	ldr r1, =FIVE_MS
	str r1, [r0]
		
// PIT1 auf eine Sekunde einstellen
	ldr r0, =PIT_LDVAL1
	ldr r1, =ONE_SECOND 
	str r1, [r0]

//PIT0/PIT1 konfigurieren
	mov r1, #PIT0_INIT			// Interrupt aktiviert, Timer nicht aktiviert
	ldr r0, =PIT_TCTRL0
	str r1, [r0]
	mov r1, #PIT1_INIT			// nicht mit PIT0 verkettet, Interrupt aktiviert, Timer aktiviert
	ldr r0, =PIT_TCTRL1
	str r1, [r0]
	
// NVIC-Bitmaske fuer PITs und Port E setzen
	ldr r0, =NVIC_ICPR2
	ldr r1, =NVIC_ISER2_MASK
	str r1, [r0]
	ldr r0, =NVIC_ISER2
	str r1, [r0]
 	
// Vorbelegung verwendeter Register
	mov r4, #0 					// Zaehler
	mov r6, #1 					// Zaehlrichtung (Default: hoch)
	mov r7, #0 					// Flag "Gestartet/Angehalten"
	ldr r8, =mod				// Adresse der Variablen mod vorhalten
	ldr r9, =pit1_flag			// Variable pit1_flag
	
mainloop:
	bl get_sw1					// Status von SW1 abfragen
	ldr r5, [r8]				// Variable mod laden
	tst r5, #1					// Bit 0 (SW1-Flag) ueberpruefen
	beq not_sw1					// Sprung, wenn SW1 nicht gedrueckt

	rsbs r6, r6, #0				// Zaehlrichtung invertieren (1 <-> -1) 
	ite pl
	ldrpl r0, =str_up			// "Hoch" auswaehlen
	ldrmi r0, =str_down			// "Runter" auswaehlen
	bl uart_putString			// Ausgabe auf dem Terminal
	b sw_end
			
not_sw1:
	tst r5, #2					// Bit 1 (SW2-Flag) ueberpruefen
	beq sw_end					// Sprung, wenn SW2 nicht gedrueckt
	eors r7, r7, #1 			// Flag fuer Gestartet/Gestoppt invertieren
	ite eq
	ldreq r0, =str_start		// "Gestartet" auswaehlen
	ldrne r0, =str_stop			// "Angehalten" auswaehlen
	bl uart_putString			// Ausgabe auf dem Terminal
	
sw_end:
	str r10, [r8]				// Variable mod auf 0 setzen

	bl uart_charPresent			// Terminalabfrage
	cbz r0, no_char				// bei no_char weitermachen, wenn kein Zeichen eingegeben

	bl uart_getChar				// Zeichen vorhanden -> abholen		
	cmp r0, #CHAR_S				// wurde ein s eingegeben?
	beq ci_s					// Ja -> Start/Stopp
	cmp r0, #CHAR_PL			// wurd ein + eingegeben?
	beq ci_plus					// Ja -> Plus
	cmp r0, #CHAR_MI			// wurde ein - eingegeben?
	beq ci_minus				// Ja -> Minus

no_char:
	ldr r0, [r9]				// PIT-Flag laden
	ands r0, r0, #1				// Bit 0 testen
	beq mainloop				// wenn es Null ist, die Hauptschleife erneut durchlaufen
	
	str r10, [r9]				// PIT Flag zuruecksetzen
	
	cmp r7, #0					// Zaehler aktiv?
	bne mainloop				// nein, die Hauptschleife erneut durchlaufen
	
	add r4, r4, r6				// Zaehler inkremetieren/dekrementieren (R6)
	
// Ausgabe der unteren vier bit des Zaehlers auf den LEDs E1 bis E4
	ldr r0, =E1					// Bit-Band-Alias-Adresse der LED E1 laden
	bic r2, r11, r4				// R2 = R11 AND NOT R4; Bit 0 (R11 = 1) des Zaehlers (R4) invertiert nach R2 schreiben 
	str r2, [r0]				// Bit auf LED ausgeben

	ldr r0, =E2
	bic r2, r11, r4, LSR #1		// R2 = R11 AND NOT (R4 >> 1); R4 wird zuerst um Eins nach rechts geschoben, 
	str r2, [r0]				// so dass effektiv Bit 1 des Zaehlers verarbeitet wird

	ldr r0, =E3
	bic r2, r11, r4, LSR #2		// R2 = R11 AND NOT (R4 >> 2); Bit 2 von R4
	str r2, [r0]

	ldr r0, =E4
	bic r2, r11, r4, LSR #3		// R2 = R11 AND NOT (R4 >> 3); Bit 3 von R4
	str r2, [r0]
	
// Ausgabe des Zaehlers auf dem Terminal
	ldr r0, =str_hex
	bl uart_putString
	mov r0, r4 
	bl uart_putByte
	mov r0, #LF
	bl uart_putChar

	b mainloop					// die Hauptschleife erneut durchlaufen

ci_minus:
	mov r6, #-1					// Zaehlrichtung "Runter" festlegen
	ldr r0, =str_down			// Und entsprechenden String auswaehlen
	bl uart_putString			// Ausgabe auf dem Terminal
	b no_char

ci_plus:
	mov r6, #1					// Zaehlrichtung "Hoch" festlegen
	ldr r0, =str_up				// und entsprechenden String auswaehlen
	bl uart_putString			// Ausgabe auf dem Terminal
	b no_char
	
ci_s:	
	eors r7, r7, #1 			// Flag fuer Gestartet/Gestoppt invertieren
	ite eq
	ldreq r0, =str_start		// "Gestartet" auswaehlen
	ldrne r0, =str_stop			// "Angehalten" auswaehlen
	bl uart_putString			// Ausgabe auf dem Terminal
	b no_char
	
	
/*** Unterprogramme und ISRs ***/

/*
 * Prueft (per Polling), ob an SW1 eine fallende Flanke aufgetreten ist und
 * stellt sicher, dass der (Low-)Impuls langer als 5 ms war.
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func 
get_sw1:
	ldr r1, =edge				// Adresse der Variablen fuer den letzten Pegel
	
	ldr r3, =SW1				// Bit-Band-Adresse für SW1-Input
	ldr r2, [r3]				// Pegel von SW1 abfragen
	cbnz r2, sw1_set_edge		// beenden, wenn Taster nicht gedrueckt (H-Pegel)
	
	ldr r0, [r1]				// letzten Pegelwert laden
	cbz r0, sw1_return			// keine Flanke, Unterprogramm verlassen, edge muss nicht angepasst werden, da gleicher Pegel
	
	ldr r0, =(FIVE_MS + 2) / 3	// Wert fuer 5 ms laden  (Erklaerung s.u.)
	
sw1_loop:
	subs r0, r0, #1
	bne sw1_loop

	/* Kostenabschaetzung (Zeit) fuer die Schleife:
		Befehl	Takte
		subs	1
		bne		2-4		Standardfall ist "Taken"; hier ist die Abschaetzung stark abhaengig von der Pipeline.
						Eine gesicherte Aussage ist nicht ohne weiteres moeglich. Daher wird zur Sicherheit der
						kleinste Wert angenommen, so dass mindesten 5 ms gewartet wird.
						 
		SUMME	3		Damit ergibt sich fuer den Schleifeninitialwert "FIVE_MS" 125000 (= 5 ms in Takten) / 3
						Da das Ergebnis nicht ganzzahlig ist, runden wir auf (+2).
						
		Eine Loesung mittels PIT ist natuerlich auch moeglich und genauer. Aber hier ist einmal die Abschaetzung mit einer Schleife
		dargelegt. Ausserdem kommt es bei fuenf Millisekunden nicht auf ein paar Takte (40 ns pro Takt) an, solange das Minimum eingehalten wird.
	*/
	
	ldr r2, [r3]				// Pegel des Tasters einlesen
	cbnz r2, sw1_set_edge		// Impuls war kuerzer als 5 ms -> raus
	
	mov r0, #1					// Wert für mod (SW1 gueltig)
	ldr r3, =mod				// Adresse mod laden
	str r0, [r3]				// speichern
		
sw1_set_edge:
	str r2, [r1]				// letzten Pegel speichern
	 
sw1_return:	
	bx lr						// Ruecksprung

/*
 * ISR fuer SW2 (Port E)
 * Parameter: keine
 * Rueckgabe: keine
 *
 * Genau genommen müsste zunächst geprüft werden, ob der IRQ tatsaechlich von SW2 ausgelöst wurde. 
 * Da im NVIC aber nur der komplette Port E freigegeben und auch nur eine ISR für Port E eingetragen werden kann,
 * müsste dann zusaetzlich noch geprueft werden, welcher (andere) Pin an Port E den IRQ ausgeloest (PORTE_ISFR) hat.
 * Auch dieser muesste entsprechend behandelt werden, da er ansonsten als "haengend" (pending) gelten und
 * diese ISR direkt wieder aufgerufen würde.
 *
 * Daher gehen wir vereinfachend davon aus, dass ein IRQ an Port E ausschließlich von SW2 ausgeloest werden kann.
 *
 */		
.thumb_func 
isr_port_e: 					
	ldr r0, =SW2_CIF			// Bit-Band-Adresse des Interrupt-Flags fuer SW2
	mov r1, #1
	str r1, [r0]				// Flag loeschen

	ldr r2, =PIT0_TEN			// PIT0: Timer Enable Bit
	ldr r3, [r2]
	cbnz r3, isr_port_e_exit	// PIT0 laeuft schon/noch
	
	str r1, [r2]				// PIT0 (5 ms) aktivieren

isr_port_e_exit:
	bx lr

/*
 * ISR fuer PIT0 (Impulslaenge an SW2 pruefen)
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func 
isr_pit0:
	mov r0, #1					
	ldr r1, =PIT_TFLG0			// Interrupt-Flag
	str r0, [r1] 				// zuruecksetzen

	mov r0, #0					// PIT0
	ldr r1, =PIT0_TEN			// Timer Enable Flag
	str r0, [r1]				// zuruecksetzen => PIT0 deaktivieren

	ldr r1, =SW2				// PDIR-BB (SW2) 
	ldr r0, [r1]				// auslesen
	cbnz r0, isr_pit0_exit		// SW2 ist nicht (mehr) gedrueckt, ISR beenden
	
	mov r0, #2					// Wert fuer SW2
	ldr r1, =mod				// nach mod
	str r0, [r1]				// schreiben
	
isr_pit0_exit:	
	bx lr	

/*
 * ISR fuer PIT1 (sekuendlich)
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func 
isr_pit1:						
	mov r0, #1					
	ldr r1, =pit1_flag			// Flag fuer PIT
	str r0, [r1]				// setzen

	ldr r1, =PIT_TFLG1			// Interrupt-Flag
	str r0, [r1] 				// zuruecksetzen
	bx lr						// Ruecksprung

		 
/*** Datenbereich (ab 0x20000000) ***/
.data
mod: .word 0
edge: .word 0
pit1_flag: .word 0
str_start: .asciz "Gestartet\n"
str_stop: .asciz "Angehalten\n"
str_up: .asciz "Hoch\n"
str_down: .asciz "Runter\n"
str_hex: .asciz "0x"


/************************************************************
Versuch: 3-1
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"			// Praktikumsspezifische Definitionen
.include "lib_uart.inc"			// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text							// hier beginnt ein Code-Segment
.align	2						// Ausrichtung an eine gerade Adresse
.global	main					// "main" wird als globales Symbol deklariert
.syntax unified

// Registeradressen
.equ SIM_SCGC3,     0x40048030
.equ SIM_SCGC5,     0x40048038
.equ SIM_SCGC6,     0x4004803C

.equ PORTA_PCR0,	0x40049000	// Start-Adresse der Port A PCR-Register
// Offsets für die einzelnen PCR-Register
.equ PCR10,	0x28				// E4
.equ PCR11,	0x2C				// E1
.equ PCR28,	0x70				// E2
.equ PCR29,	0x74				// E3

.equ ADC1_SC1A, 	0x400BB000
.equ ADC1_CFG1, 	0x400BB008
.equ ADC1_CFG2, 	0x400BB00C
.equ ADC1_RA, 		0x400BB010
.equ ADC1_SC2, 		0x400BB020  
.equ ADC1_SC3, 		0x400BB024
.equ ADC1_PGA, 		0x400BB050
.equ ADC1_PG, 		0x400BB02c
.equ ADC1_CLPD, 	0x400BB034	// lt. NXP (Freescale) ist CLPD nur bei diff. Messung relevant,
//.equ ADC1_CLPS, 	0x400BB038	// daher lassen wir für single-ended beide Alternativen zu
.equ ADC1_CLP0, 	0x400BB04C

.equ GPIOA_PDOR,    0x400FF000
.equ GPIOA_PDDR,    0x400FF014

.equ PIT_MCR,       0x40037000
.equ PIT_LDVAL1,    0x40037110
.equ PIT_TCTRL1,    0x40037118
.equ PIT_TFLG1,     0x4003711C

.equ NVIC_ISER2, 	0xE000E108
.equ NVIC_ICPR2, 	0xE000E288
.equ SCR_VTOR,		0xE000ED08

// Bit Band-Konstanten
__BBREG	SIM_ADC1, SIM_SCGC3, 27
__BBREG SIM_PORTA, SIM_SCGC5, 9
__BBREG SIM_PIT, SIM_SCGC6, 23

__BBREG	E1, GPIOA_PDOR, 11
__BBREG	E2, GPIOA_PDOR, 28
__BBREG	E3, GPIOA_PDOR, 29
__BBREG	E4, GPIOA_PDOR, 10

__BBREG ADC1_SC1A_COCO, ADC1_SC1A, 7
__BBREG ADC1_SC3_CALF, ADC1_SC3, 6

// weitere Konstanten
.equ ADC1_CFG1_VAL16, 0x6c		// kein Low-Power, Takt durch 8 geteilt, kurze Messung, 16 bit Auflösung, Bus-Takt
.equ ADC1_CFG1_VAL12, 0x64		// kein Low-Power, Takt durch 8 geteilt, kurze Messung, 12 bit Auflösung, Bus-Takt
.equ ADC1_CFG1_VAL10, 0x68		// kein Low-Power, Takt durch 8 geteilt, kurze Messung, 10 bit Auflösung, Bus-Takt
.equ ADC1_CFG1_VAL08, 0x60		// kein Low-Power, Takt durch 8 geteilt, kurze Messung,  8 bit Auflösung, Bus-Takt
.equ ADC1_SC1A_VAL, 0x14 		// kein Interrupt, massebezogene Messung (single ended), Kanal AD20
.equ ADC1_SC1A_VAL_DISABLE, 0x1F// Modul deaktiviert
.equ ADC1_SC3_VAL, 0xE 			// keine Kalibrierung, laufende Wandlung, Hardware Mittelwertbildung, 16 Mittelwerte
.equ ADC1_SC3_CVAL, 0xCF		// Kalibrierung starten, laufende Wandlung, Hardware Mittelwertbildung, 32 Mittelwerte, CALF löschen

.equ NVIC_ISER2_MASK, 0x20		// PIT1

.equ IVT_PIT1_OFS, 0x154		// Offset der IVT-Vektor-Adresse von PIT1

.equ ONE_SECOND, 25000000-1		// 25.000.000 Takte bei 25 MHz
.equ TCTRL1_VAL, 3				// nicht mit PIT0 verkettet, Interrupt aktiviert, Timer aktiviert	
.equ BIT15, 0x8000				// Bit 15 Maske
.equ MASK_CAL, 0x80				// Maske CAL-Bit
.equ NR_CLP, ((ADC1_CLP0 - ADC1_CLPD)>>2)+1	// Anzahl der CLPx-Register

.equ GPIO_MODE, 0x120 			// GPIO-Modus, Open-Drain
.equ PDDR_MASK_LED, (1<<10 | 1<<11 | 1<<28 | 1<<29)	// Maske für die LEDs E1 bis E4

.equ LF, 	0x0A				// Line Feed (neue Zeile)
.equ CHAR_0, '0'
.equ CHAR_3, '3'
.equ CHAR_S, 's'
.equ CHAR_X, 'x'
.equ COLON, ':'
.equ PAR_R, ')'
.equ SPC, ' '

// Werte der ADC-Auflösung und mV-Schrittweiten (Vmax (3300 mV) / Auflösung) (gerundet)
.equ RES8, 8
.equ MV8, 413
.equ RES10, 10
.equ MV10, 330
.equ RES12, 12
.equ MV12, 275
.equ RES16, 16
.equ MV16, 206 

/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchführt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind.
*/
main:	
// Initialisiere UART (Terminal) 
	bl uart_init

// Für die Initialisierung haufig verwendete Werte vorbelegen	
	mov r10, #0
	mov r11, #1
	
// Takt für die benutzten Module aktivieren
	ldr r0, =SIM_ADC1
	str r11, [r0]
	ldr r0, =SIM_PIT
	str r11, [r0]
	ldr r0, =SIM_PORTA
	str r11, [r0]

adc1_init:	
// Konfiguration von ADC1
	ldr r0, =ADC1_PGA
	str r10, [r0]				// PGA deaktiviert
	ldr r12, =ADC1_CFG1			// hohes Register (vorladen)
	mov r1, #ADC1_CFG1_VAL16	// Standard: 16-bit Auflösung
	str r1, [r12]
	ldr r0, =ADC1_CFG2
	str r10, [r0]				// Default: Reset-Werte
	ldr r0, =ADC1_SC2
	str r10, [r0]				// Software-Trigger, kein Compare, kein DMA, Default VRef
	
	bl adc1_calib				// ADC1 kalibrieren
	cbz r0, cal_ok				// Fehler aufgetreten? nein => weiter

	ldr r0, =str_calerr			// Fehlermeldung zur Kalibrierung ausgeben
	bl uart_putString
	b adc1_init					// nochmal versuchen (könnte um einen Versuchszähler mit Abbruchbedingung erweitert werden)
	
cal_ok:
	ldr r0, =ADC1_SC3
	mov r1, #ADC1_SC3_VAL		// Normalbetrieb
	str r1, [r0]
	ldr r9, =ADC1_SC1A			// hohes Register (vorladen)
	mov r1, #ADC1_SC1A_VAL		// Wandlung starten
	str r1, [r9]				
	
// NVIC-Bitmaske für PIT1 setzen
	mov r0, #NVIC_ISER2_MASK
	ldr r1, =NVIC_ICPR2 
	str r0, [r1]
	ldr r1, =NVIC_ISER2 
	str r0, [r1]
	
// Adresse der ISR für PIT1 in die IVT eintragen
	ldr r0, =SCR_VTOR
	ldr r1, [r0]
	ldr r0, =isr_pit1
	str r0, [r1, #IVT_PIT1_OFS]

// PIT-Modul konfigurieren
	ldr r0, =PIT_MCR
	str r11, [r0]				// FRZ=1, MDIS=0 (PIT stopped in Debug mode, PIT-Clock enabled) 
	
// PIT1 auf eine Sekunde einstellen
	ldr r0, =PIT_LDVAL1
	ldr r1, =ONE_SECOND 
	str r1, [r0]
	
//PIT1 Konfigurieren
	ldr r0, =PIT_TCTRL1
	mov r1, #TCTRL1_VAL
	str r1, [r0]
	
// GPIO-Pins konfigurieren
	ldr r0, =PORTA_PCR0
	mov r1, #GPIO_MODE		// GPIO-Modus mit Open-Drain festlegen
	str r1, [r0, #PCR11]	// LED E1
	str r1, [r0, #PCR28]	// LED E2
	str r1, [r0, #PCR29]	// LED E3
	str r1, [r0, #PCR10]	// LED E4
	
// Datenrichtung der GPIO-Pins festlegen
	ldr r0, =PDDR_MASK_LED
	ldr r1, =GPIOA_PDDR
	ldr r2, [r1]
	orr	r2, r2, r0
	str r2, [r1]

// Vorbelegung verwendeter Register
	mov r4, #1 					// Flag für Start/Stopp in R4
	mov r5, #3 					// Index der Anfangs-Auflösung (16 Bit) in R5
	ldr r6, =pit_flag			// Adresse der Variablen pit_flag
	ldr r7, =ADC1_RA			// Ergebnisregister des ADC1
	ldr r8, =tab_modus			// Anfangsadresse der Tabelle für die Auflösung
	ldr r10, =ADC1_SC1A_COCO	// Bit-Band-Alias-Adresse des COCO-Bits
	// R9: ADC1_SC1A
	// R11: 1-Konstante
	// R12: ADC1_CFG1
	
mainloop:
	cmp r4, #0					// Stopp-Modus?
	beq no_output				// ja, dann keine Ausgabe auf dem Terminal

wait4coco:
	ldr r0, [r10]				// Status der A/D-Wandlung
	cmp r0, #0					// Wandlung beendet?
	beq wait4coco				// nein, also weiter warten

	ldr r0, [r7] 				// aktuelles Egebnis der A/D-Wandlung auslesen
	 
	ldr r1, [r6]				// PIT1 abgelaufen?
	cbz r1, no_output			// nein, also keine Ausgabe auf dem Terminal
	
	mov r3, #0					// PIT-Flag
	str r3, [r6]				// zurücksetzen
	
	mov r1, r5					// Auflösung (R1: für Stufenberechnung) und A/D-Wert (R0) 
	bl output					// auf dem Terminal und den LEDs ausgeben
		
no_output:
	bl uart_charPresent			// wurde ein Zeichen eingegeben?
	cmp r0, #0
	beq mainloop				// nein, also zurück zur Hautschleife
	
	bl uart_getChar				// Zeichen einlesen
	
	cmp r0, #CHAR_S				// wurde ein 's' eingegeben?
	bne no_s					// nein, also verzweigen

	eors r4, r4, #1				// R4 negieren, Status setzen
	ittee eq					// 1: Wandlung läuft, 0: Wandlung angehalten
	moveq r1, #ADC1_SC1A_VAL_DISABLE
	ldreq r0, =str_stop
	movne r1, #ADC1_SC1A_VAL
	ldrne r0, =str_start
	
	str r1, [r9]				// selektierten Wert ins Konfigurationsregister (SC1A) schreiben
	bl uart_putString			// entsprechenden String (R0) ausgeben
	b mainloop

no_s:
	cmp r4, #0					// Stopp-Modus aktiv?
	bne mainloop				// nein, also keine Änderungen der Auflösung zulassen
	
	cmp r0, #CHAR_0				// kleiner Null?
	blo mainloop	
	cmp r0, #CHAR_3				// größer Drei?
	bhi mainloop

	and r5, r0, #3				// Index für Auflösung erzeugen (maskieren)
	add r2, r8, r5, lsl #3 		// Adresse des Tablleneintrags bestimmen
	ldrd r0, r1, [r2]			// Anfangsadresse & Konfiguration  des Strings aus der Tabelle (R8) laden
	str r1, [r12]				// Konfiguration (Auflösung) setzen
	bl uart_putString			// gewählte Auflösung ausgeben
	b mainloop
	

/*** ISR und Unterprogramme ***/

/*
 * ISR für PIT1 (sekündlich)
 * Parameter: keine
 * Rückgabe: keine
 */		
.thumb_func	
isr_pit1:						// ISR für PIT1
	mov r0, #1					
	ldr r1, =pit_flag			// Flag für PIT
	str r0, [r1]				// setzen
	ldr r1, =PIT_TFLG1			// Interrupt-Flag
	str r0, [r1] 				// zurücksetzen
	bx lr						// Rücksprung

/* 
 * Kalibriert ADC1
 * Parameter: keine
 * Rückgabe: r0: Fehler-Flag
 */
.thumb_func
adc1_calib:
	ldr r0, =ADC1_SC3
	mov r1, #ADC1_SC3_CVAL		// Kalibrierung starten, CALF löschen
	str r1, [r0]
	
wait4cal:
	ldr r1, [r0]				// Status der Kalibrierung
	ands r1, r1, #MASK_CAL		// Kalibrierung beendet? (alternativ kann auch auf COCO gewartet werden)
	bne wait4cal				// nein, also weiter warten
	
	ldr r1, =ADC1_SC3_CALF		
	ldr r0, [r1]
	cbnz r0, cal_f				// Kalibrierung normal beendet? nein => mit Fehler-Flag beenden

	ldr r2, =ADC1_CLPD			// erstes Register mit Kalibrierungsergebnissen
	mov r0, #NR_CLP				// die Registerinhalte (CLP0-CLP4 und CLPD[o. CLPS]) müssen akkumuliert werden
	mov r3, #0					// Summe auf Null setzen
cal_loop:
	ldr r1, [r2], #4			// Kalibrierungswert laden, Post-Index auf nächstes Register
	add r3, r3, r1				// Werte akkumulieren
	subs r0, r0, #1
	bne cal_loop				// alle Werte durchlaufen? (R0=0: kein Fehler)
	
	lsr r3, r3, #1				// Summe der Werte duch zwei dividieren
	orr r3, r3, #BIT15			// Bit 15 setzen
	
	ldr r2, =ADC1_PG			// Kalibrierungswerte
	str r3, [r2]				// eintragen
	
cal_f:
	bx lr

/*
 * Gibt den ADC-Wert, Spannungsbereich und Stufe auf dem Terminal aus und 
 * die zeigt die aktuelle Stufe auf den LEDs an.
 * Parameter: r0: ADC-Wert, r1: Auflösung	
 * Rückgabe: keine
 */		
.thumb_func	
output: 					
	push {r4-r5,lr}				// LR und benötigte hohe Register sichern, da weitere UP-Aufrufe erfolgen
	
	mov r4, r0					// ADC-Wert sichern
	mov r5, r1					// R1 nach R5 schieben, da R1 nach UP-Aufruf ungültig wird

	mov r0, #CHAR_0				
	bl uart_putChar				
	mov r0, #CHAR_X				// 0 und x
	bl uart_putChar				// ausgeben
	
	mov r0, r4
	bl uart_putInt16			// A/D-Wert als 16-bit-Wert ausgeben
	
	mov r0, #COLON				// Doppelpunkt
	bl uart_putChar				// und
	mov r0, #SPC				// Leerzeichen
	bl uart_putChar				// ausgeben
	
// Stufe berechnen
	ldr r3, =tab_stufen			// Startadresse der Tabelle
	add r3, r3, r5, lsl #3		// Startadresse des Eintrags festlegen

	ldrd r0, r2, [r3]		 	// Anzahl der Bits und mV/Stufe laden
	
	mul r1, r4, r0				// Multiplikation zum Ausgleich des Rundungsfehlers
	lsr r5, r1, r0				// Stufe in R5 (Division mit einer Zweierpotenz durch Verschieben ersetzen)

//  Grenzen berechnen
	mul r0, r2, r5 				// untere Bereichsgrenze = (Stufe * mV)
	add r4, r0, r2				// obere BG = untere BG + Intervallgröße - 1 (- 1 wird nachgeholt, s. u.)

	bl output_dez				// untere Bereichsgrenze dezimal ausgeben
	
	ldr r0, =str_trenner		// Bereichstrenner ausgeben
	bl uart_putString 

	sub r0, r4, #1				// obere Bereichsgrenze anpassen (- 1) und ausgeben
	bl output_dez

	ldr r0, =str_mv_stufe		// Zusatztext ausgeben
	bl uart_putString 

	mov r0, r5					// Stufe ausgeben
	bl uart_putByteBase10
	
	mov r0, #PAR_R				// schließende Klammer
	bl uart_putChar				
	mov r0, #LF					// und LF
	bl uart_putChar				// ausgeben
	
// Ausgabe der Stufe (R5) auf den LEDs E1 bis E4
	ldr r1, =E1					// Bit-Band-Alias-Adresse der LED E1 laden
	mov r3, #1
	bic r2, r3, r5				// R2 = R3 AND NOT R5; Bit 0 von R5 invertiert nach R2 schreiben 
	str r2, [r1]				// Bit auf LED ausgeben
	ldr r1, =E2
	bic r2, r3, r5, lsr #1		// R2 = R3 AND NOT (R5 >> 1); R5 wird zuerst um Eins nach rechts geschoben, 
	str r2, [r1]				// so dass effektiv Bit 1 der Stufe verarbeitet wird
	ldr r1, =E3
	bic r2, r3, r5, lsr #2		// R2 = R3 AND NOT (R5 >> 2); Bit 2 von R5
	str r2, [r1]
	ldr r1, =E4
	bic r2, r3, r5, lsr #3		// R2 = R3 AND NOT (R5 >> 3); Bit 3 von R5
	str r2, [r1]
		
	pop {r4-r5,pc}				// gesicherte Register wiederherstellen und Rücksprung

/*
 * Gibt einen Wert von 0000 bis 9999 inkl. führender Nullen dezimal auf dem Terminal aus
 * Parameter: r0: Wert
 * Rückgabe: keine 
*/
.thumb_func
output_dez:
	push {r4,lr}
	
	mov r1, #100				// Divisor bzw. Multiplikator
	udiv r2, r0, r1				// Wert / 100
	mls	r4, r2, r1, r0			// Eingabewert(R0) - Quotient(R2) * 100(R1)

	mov r0, r2
	cmp r0, #0x9				// führende Null notwendig?
	ite ls						// passendes Unterprogramm (mit/ohne führende(r) Null) auswählen
	ldrls r1, =uart_putByte
	ldrhi r1, =uart_putByteBase10
	blx r1 						// selektiertes Unterprogramm aufrufen

	mov r0, r4
	cmp r0, #0x9
	ite ls
	ldrls r1, =uart_putByte
	ldrhi r1, =uart_putByteBase10
	blx r1 
	
	pop {r4,pc}


/*** Datenbereich (ab 0x20000000) ***/
.data

// Tabelle mit den Startadressen der Strings und Konfigurationen für die Auflösung
tab_modus: .word str_res08, ADC1_CFG1_VAL08, str_res10, ADC1_CFG1_VAL10, str_res12, ADC1_CFG1_VAL12, str_res16, ADC1_CFG1_VAL16

// Tabelle mit ADC-Auflösung und mV-Schrittweiten (Vmax (3300 mV) / Auflösung) (gerundet)
tab_stufen: .word RES8, MV8, RES10, MV10, RES12, MV12, RES16, MV16 

pit_flag: .word 0x0

str_trenner: .asciz " - "
str_mv_stufe: .asciz " mV (Stufe "

str_start: .asciz "A/D-Wandler gestartet\n"
str_stop: .asciz "A/D-Wandler angehalten (Aufloesung aenderbar)\n"
str_calerr: .asciz "Kalibrierung fehlgeschlagen!\n"

str_res08: .asciz "Aufloesung: 8 bit\n"
str_res10: .asciz "Aufloesung: 10 bit\n"
str_res12: .asciz "Aufloesung: 12 bit\n"
str_res16: .asciz "Aufloesung: 16 bit\n"


/************************************************************
Versuch: 4-1
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"				// Praktikumsspezifische Definitionen
.include "lib_uart.inc"				// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text								// hier beginnt ein Code-Segment
.align	2							// Ausrichtung an eine gerade Adresse
.global	main						// "main" wird als globales Symbol deklariert
.syntax unified

// Registeradressen
.equ SIM_SCGC4, 	0x40048034
.equ SIM_SCGC5,     0x40048038
.equ SIM_SCGC6,		0x4004803C

.equ PIT_MCR,		0x40037000
.equ PIT_LDVAL1,    0x40037110
.equ PIT_TCTRL1,    0x40037118
.equ PIT_TFLG1,     0x4003711C

.equ I2C0_F,		0x40066001
.equ I2C0_C1,		0x40066002
.equ I2C0_S,		0x40066003
.equ I2C0_D,		0x40066004
.equ I2C0_C2,		0x40066005
.equ I2C0_FLT,		0x40066006
.equ I2C0_RA,		0x40066007
.equ I2C0_SMB, 		0x40066008
.equ I2C0_SLTH,		0x4006600A
.equ I2C0_SLTL,		0x4006600B

.equ PORTD_PCR0,	0x4004C000		// I2C: SCL & SDA
.equ PCR8, 0x20		
.equ PCR9, 0x24

.equ GPIOD_PDOR,    0x400FF0C0
.equ GPIOD_PDIR,    0x400FF0D0
.equ GPIOD_PDDR,    0x400FF0D4

.equ NVIC_ISER2, 	0xe000e108
.equ NVIC_ICPR2, 	0xe000e288

.equ SCR_VTOR,		0xE000ED08		// Register fuer die Interrupt-Vektor-Tabelle (IVT)

// Bit Band-Konstanten
__BBREG SIM_PORTD, SIM_SCGC5, 12
__BBREG	SIM_PIT, SIM_SCGC6, 23
__BBREG SIM_I2C0, SIM_SCGC4, 6

__BBREG I2C0_C1_TXAK, I2C0_C1, 3
__BBREG I2C0_C1_TX, I2C0_C1, 4
__BBREG I2C0_C1_MST, I2C0_C1, 5

__BBREG I2C0_S_IICIF, I2C0_S, 1
__BBREG I2C0_S_ARBL, I2C0_S, 4

// weitere Konstanten
.equ NVIC_ISER2_MASK, 0x20			// Bit 5 (PIT1)

.equ IVT_OFS_PIT1, 0x154			// IVT-Offset fuer PIT1

.equ I2C0_F_VAL, 0x12  				// ~400 kHz (max. fuer DS75) 
.equ I2C0_C1_INIT, 0x80 			// IICEN = 1 -> I2C Modul aktiviert
.equ I2C0_C1_START, 0xB0			// IICEN=1: I2C Enable, IICIE=0: Interrupt disable, MST=1: Master Mode, TX=1: Transmit Mode Select
									// TXAK=0, RSTA=0, WUEN=0, DMAEN=0
.equ I2C0_C2_INIT, 0x00				// General Call Address disabled, 7-bit Adresse, Normal drive mode, Slave baud rate follows master, 
									// Range mode disabled, Slave address extension not used
.equ I2C0_SMB_INIT, 0x0A			// SLTF & SHTF2 loeschen

.equ I2C_C1_MST_BIT, 0x20

.equ MODE_I2C, 0x220				// PCR: Alternative 2 (I2C, Open-Drain-Enable)
.equ MODE_IO, 0x120					// PCR: GPIO, Open-Drain-Enable

.equ BIT_8, 0x100					// Bit 8
.equ BIT_8_9, 0x300					// Bits 8 & 9

.equ DS75_ADDRESS, 0x90
.equ DS75_REG_TEMP, 0x0
.equ DS75_REG_CONFIG, 0x1
.equ DS75_REG_HYST, 0x2
.equ DS75_REG_OS, 0x3

.equ DS75_CONFIG, 0x60				// Aufloesung: 12 bit, Fehlertrigger: 1, OS-Polaritaet: aktiv Low, Comparator-Modus, aktiviert

.equ T_HYST, 0x2					// Hysteresewerte
.equ T_OS, 0x3

.equ ONE_SECOND, 25000000-1			// PIT-Zyklen fuer eine Sekunde

.equ DELAY_CNT, 40					// Zaehler fuer ca. 5 µs Verzoegerung
.equ MAX_TRY, 10					// max. 10 Versuche
 
/*
* WAIT_IICIF (Timeout Berechnung)
* I2C-Bus: 9 Takte (Byte + ACK) => ~10 Takte (vereinfachend) -> 100 kHz: 100 µs; 400 kHz: 25 µs
* IICIF-Test: (7 + 7 (wenn wiederholt) pro Schleifendurchlauf + 4) Takte a 40 ns 
* => 280 ns + 280 ns pro Schleifendurchlauf + 160 ns => max. (n + 1) * 280 ns + 160 ns
* Da es sich hier um einen Timeout handelt, muessen wir auf jeden Fall eine (sinnvolle) Reserve einplanen.
* Wir berechnen nur die Anzahl der benoetigten Schleifendurchlaeufe und runden dann noch (sehr) grosszuegig auf:
* - 400 kHz: 25.000 ns / 280 ns = 89,27 => 128
* - 100 kHz (100 µs): 357,14 Durchlaeufe => 512 
*/
.equ WAIT_IICIF, 128				// Anzahl Durchlaeufe bei 400 kHz

/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind.
*/
main:
	
	bl uart_init					// Terminal initialisieren
	
	mov r10, #0						// zur Vereinfachung der Initialisierung
	mov r11, #1

// Takt fuer die benoetigten Module aktivieren	
	ldr r0, =SIM_I2C0				// Takt fuer I2C-Modul aktivieren
	str r11, [r0]
	ldr r0, =SIM_PIT				// Takt fuer PIT-Modul aktivieren
	str r11, [r0]
		
// Adresse der ISRs fuer PIT1 in die IVT eintragen
	ldr r0, =SCR_VTOR
	ldr r1, [r0]
	ldr r0, =pit1_isr
	str r0, [r1, #IVT_OFS_PIT1]
	
// PIT-Modul konfigurieren
	ldr r0, =PIT_MCR
	str r11, [r0]					// FRZ=1, MDIS=0 (PIT stopped in Debug mode, PIT-Clock enabled) 
	
// PIT1 auf eine Sekunde einstellen
	ldr r0, =PIT_LDVAL1
	ldr r1, =ONE_SECOND 
	str r1, [r0]

// NVIC-Bitmaske fuer PIT1 setzen und pending Interrupts loeschen
	ldr r0, =NVIC_ICPR2
	mov r1, #NVIC_ISER2_MASK
	str r1, [r0]
	ldr r0, =NVIC_ISER2
	str r1, [r0]
 	
//I2C-Schnittstelle zuruecksetzen
i2c_reset:
	bl i2c_Bus_Reset				// den I2C-Bus manuell (per GPIO) zuruecksetzen
	cbz r0, i2c_gpio_ok
	ldr r0, =i2c_gpio_err_str		// String mit Fehlermeldung
	bl uart_putString				// ausgeben
i2c_gpio_not_ok:
	b i2c_gpio_not_ok				// Endlosschleife

//I2C-Schnittstelle konfigurieren
i2c_gpio_ok:
	bl i2c_init						// I2C0 initialisieren
	
	mov r0, #((DS75_REG_CONFIG << 8) | DS75_CONFIG)	// DS75 konfigurieren
	movt r0, #(1<<8)				// MSB-Index eintragen
	bl ds75_write
	tst r0, #1						// Fehlerflag testen
	bne i2c_reset					// I2C-Reset bei Fehler
	
	mov r0, #DS75_REG_TEMP			// DS75 Temperatur-Register
	bl ds75_write					// auswaehlen
	tst r0, #1						// Fehlerflag testen
	bne i2c_reset					// I2C-Reset bei Fehler

	bl ds75_read16					// 2 Byte lesen (Temperatur)
	tst r0, #1						// Fehlerflag testen
	bne i2c_reset					// I2C-Reset bei Fehler
	mov r4, r0						// Messwert sichern

	add r0, r4, #(T_OS<<8)			// Messwert + O.S.
	movt r0, #((2<<8) | DS75_REG_OS) // MSB-Index und Register eintragen
	bl ds75_write				 
	tst r0, #1						// Fehlerflag testen
	bne i2c_reset					// I2C-Reset bei Fehler
	
	add r0, r4, #(T_HYST<<8)		// Messwert + Hysterese
	movt r0, #((2<<8) | DS75_REG_HYST)	// MSB-Index und Register eintragen
	bl ds75_write					// als Hysterese eintragen
	tst r0, #1						// Fehlerflag testen
	bne i2c_reset					// I2C-Reset bei Fehler
	
// PIT1 starten
	ldr r0, =PIT_TCTRL1
	mov r1, #3						// nicht mit PIT0 verkettet, Interrupt aktiviert, Timer aktiviert
	str r1, [r0]
	
	mov r0, #DS75_REG_TEMP			// Pointer fuer das Temperatur-Register
	bl ds75_write
	tst r0, #1						// Fehlerflag testen
	bne i2c_reset					// I2C-Reset bei Fehler
	
	ldr r4, =timer_flag				// Register vorbelegen

mainloop:
	ldr r1, [r4]					// Timer Flag lesen
	cmp r1, #0
	beq mainloop					// weiter warten, wenn Flag nicht gesetzt
	
	str r10, [r4] 					// Timer Flag zuruecksetzen
	bl ds75_read16					// 2 Byte von I2C-Bus lesen
	tst r0, #1						// Fehlerflag testen
	bne i2c_reset					// I2C-Reset bei Fehler
	lsl r0, r0, #8 					// Ergebnis nach links schieben, so dass das obere Halbwort den ganzzahligen Anteil enthaelt,
									// das untere enthaelt dann die Nachkommastellen (16.16 Fixpunkt)
	bl print_Temp					// Temperatur ausgeben
	b mainloop						// zurueck zur Hauptschleife

/*** ISR und Unterprogramme ***/

/*
 * PIT1: ISR (sekuendlich)
 * Parameter: keine
 * Rueckgabe: keine
*/	
.thumb_func
pit1_isr:
	ldr r0, =timer_flag				// Timer Flag
	mov r1, #0x1
	str r1, [r0]					// setzen
	ldr r0, =PIT_TFLG1				// PIT IRQ
	str r1, [r0]					// zuruecksetzen
	bx lr

/*
 * I2C-Bus: Reset, setzt den I2C-Bus manuell (per GPIO) zurueck
 * Parameter: keine
 * Rueckgabe: r0: 0 = kein Fehler, 1 = Fehler
*/		
.thumb_func
i2c_Bus_Reset:
	push {r4,lr}
	ldr r0, =SIM_PORTD 				// In SCGC5 den Takt fuer PortD aktivieren
	mov r1, #1
	str r1, [r0]
	ldr r0, =PORTD_PCR0				// Startadresse PORTD_PCR
	mov r2, #MODE_IO				// Portleitungen PCR8, PCR9 als GPIO
	str r2, [r0, #PCR8]				// Pins PC8 und PC9
	str r2, [r0, #PCR9]
	ldr r0, =GPIOD_PDDR				// PC8, PC9 als Ausgang schalten
	ldr r2, [r0]
	orr r2, r2, #BIT_8_9
	str r2, [r0]
	mov r4, #MAX_TRY				// Anzahl Versuche
i2c_BR_Loop:
	ldr r0, =GPIOD_PDOR 			// SCL deaktivieren
	ldr r2, [r0]
	bic r2, r2, #BIT_8 				// entsprechendes Bit loeschen
	str r2, [r0]
	bl i2c_Delay
	ldr r0, =GPIOD_PDOR 			// SCL, SDA: H-Pegel setzen
	ldr r2, [r0]
	orr r2, r2, #BIT_8_9
	str r2, [r0]
	bl i2c_Delay

	ldr r0, =GPIOD_PDIR				// Port D einlesen
	ldr r2, [r0]
	and r2, r2, #BIT_8_9			// nur Portbits 9 und 10 pruefen
	cmp r2, #BIT_8_9				// Bit 9 und Bit 10 gesetzt? (also auf H-Pegel)
	ITEE eq
	moveq r0, #0					// Fehler-Flag zuruecksetzen 
	movne r0, #1					// Fehler-Flag setzen 
	subsne r4, r4, #1				// Versuchszaehler dekrementieren
	bne i2c_BR_Loop					// nochmal versuchen
	
	pop {r4,pc}

/*
 * I2C0-Initialisierung
 * Parameter: keine
 * Rueckgabe: keine
 * I2C0_A1, I2C0_A2: nicht relevant, da der K60 nur als Master arbeitet
 */
.thumb_func
i2c_init:
	ldr r0, =PORTD_PCR0				// Startadresse PORTD_PCR
	mov r2, #MODE_I2C				// Alternative 2: I2C
	str r2, [r0, #PCR8]				// Pins PC8 und PC9
	str r2, [r0, #PCR9]
	
	mov r2, #0
	ldr r0, =I2C0_RA				// Range Slave Address deaktivieren
	strb r2, [r0]				
	
	ldr r0, =I2C0_SLTH				// SCL Low Timeout deaktivieren
	strb r2, [r0]
	ldr r0, =I2C0_SLTL			
	strb r2, [r0]

	ldr r0, =I2C0_SMB				// SMB Funktionalitaet deaktivieren
	mov r1, #I2C0_SMB_INIT			// SLTF & SHTF2 loeschen
	strb r1, [r0]
	
	ldr r0, =I2C0_F					// I2C Frequency Divider Register
	mov r1, #I2C0_F_VAL				// I2C Busfrequenz einstellen
	strb r1, [r0]

	ldr r0, =I2C0_C2				// I2C Conrol Register 2
//	mov r1, #I2C0_C2_INIT			// zusaetzliche Einstellungen, falls nicht Null
	strb r2, [r0]					// 0: Kein General Call, Normal Drive Mode, SBR durch Master, Range Mode deaktiviert, 7-bit-Adressen
	
	ldr r0, =I2C0_FLT				// I2C Glitch Filter
	strb r2, [r0]					// kein Filter 
	
	ldr r0, =I2C0_C1				// I2C Conrol Register 1
	mov r1, #I2C0_C1_INIT			// I2C-Modul
	strb r1, [r0]					// aktivieren

	bx lr
	
/*
 * Verzoegerung: ca. 125 takte = 5 Mikrosekunden (inklusive Aufruf und Ruecksprung)
 * Die Verzoegerung dient in erster Linie dazu, die Zeit fuer t_BUF (1,3 µs, s. DS75-Datenblatt, S. 5)
 * einzuhalten. Wird auch verwendet, um im GPIO-Betrieb die Leitungspegel einen stabilen 
 * Zustand annehmen zu lassen. 
 * Parameter: keine
 * Rueckgabe: keine
*/		
.thumb_func
i2c_Delay:
	mov r0, #DELAY_CNT				// Zaehler initialisieren
i2c_d_loop:
	subs r0, #1
	bne i2c_d_loop
	nop
	bx lr

/*
 * I2C-Bus: START
 * Parameter: keine
 * Rueckgabe: r0: 0 = kein Fehler, 1 = Fehler
*/	
.thumb_func
i2c_Start:
	mov r0 ,#0						// Fehler-Flag zuruecksetzen
	mov r2, #MAX_TRY				// Anzahl Versuche
	ldr r3, =I2C0_C1				// I2C Control Register
i2c_Start_Loop:
	mov r1, #I2C0_C1_START			// Master & TX aktivieren
	strb r1, [r3]
	ldrb r1, [r3]					// aktuellen Wert zum Pruefen laden
	tst r1, #I2C_C1_MST_BIT			// konnte der Master Mode aktiviert werden?
	bne i2c_Start_OK				// falls ja, beenden
	subs r2, r2, #1					// Versuchszaehler dekrementieren
	bne i2c_Start_Loop				// nochmal versuchen
	ldr r0, =i2c_mst_err_str		// Fehlermeldung ausgeben
	bl uart_putString
	mov r0, #1						// Flag fuer Fehler setzen
i2c_Start_OK:
	bx lr
	
/*
 * I2C-Bus: STOP
 * Parameter: keine
 * Rueckgabe: keine
*/	
.thumb_func
i2c_Stop:
	push {lr}
	ldr r0, =I2C0_C1_MST			// MST-Bit (I2C Control Register)
	mov r1, #0
	strb r1, [r0]					// und zurueckschreiben
	bl i2c_Delay					// t_BUF(DS75) einhalten
	pop {pc}
	
/*
 * ds75_read16: liest zwei Byte vom DS75 
 * Parameter: keine
 * Rueckgabe: r0 = 16-bit-Wert (VVN0) oder bei Fehler r0 = 1
*/ 
.thumb_func
ds75_read16:	
	push {r4, r5, lr}
	mov r4, #(DS75_ADDRESS | 1)		// Slave Adresse (Lesen) erzeugen
	bl i2c_Start					// I2C-Bus-Start
	cbnz r0, ds75_read16_err		// R0 = 1? => Fehler nach oben reichen
	ldr r5, =I2C0_D
	strb r4, [r5]					// Slave Adresse auf den I2C-Bus legen 
	bl i2c_Wait_Complete			// warten, bis die Uebertragung beendet ist
	cbnz r0, ds75_read16_err		// R0 = 1? => Fehler nach oben reichen
	ldr r1, =I2C0_C1_TX				// TX-Bit loeschen 
	mov r0, #0
	strb r0, [r1]
	ldrb r1, [r5]					// Datum vom I2C-Bus lesen (*** Dummy-Read ***)
	bl i2c_Wait_Complete			// warten, bis die Uebertragung beendet ist
	cbnz r0, ds75_read16_err		// R0 = 1? => Fehler nach oben reichen
	ldr r1, =I2C0_C1_TXAK			// TXAK-Bit
	mov r0, #1						// setzen -> NACK senden
	strb r0, [r1]
	ldrb r4, [r5]					// 1. Byte von I2C-Bus lesen
	bl i2c_Wait_Complete			// warten, bis die Uebertragung beendet ist
	cbnz r0, ds75_read16_err		// R0 = 1? => Fehler nach oben reichen
	bl i2c_Stop						// I2C-Bus-Stopp
	ldrb r5, [r5]					// 2. Byte von I2C-Bus lesen
	orr r0, r5, r4, LSL #8			// beide Bytes zu einem Rueckgabewert zusammensetzen
ds75_read16_err:
	pop {r4, r5, pc}

/*
 * I2C-Bus: schreibt ein bis drei Byte auf den I2C-Bus
 * Parameter: r0 = 0x0NB3B2B1, N = Index (0..2) auf MSB, Bn = Daten
 * Rueckgabe: r0: 0 = kein Fehler, 1 = Fehler
 */	
.thumb_func
ds75_write:
	push {r4-r6,lr}
	mov r4, r0						// Parameter sichern
	ubfx r5, r0, #24, #8			// Anzahl (0..2) Bytes extrahieren
	bl i2c_Start					// I2C-Bus-Start
	cbnz r0, ds75_write_err			// R0 = 1? => Fehler nach oben reichen
	mov r0, #DS75_ADDRESS
	ldr r6, =I2C0_D					// Zeiger auf das I2C-Datenregister
	strb r0, [r6]					// Slave Adresse auf den I2C-Bus legen 
	bl i2c_Wait_Complete			// warten, bis die Uebertragung beendet ist
	cbnz r0, ds75_write_err			// R0 = 1? => Fehler nach oben reichen
ds75_write_loop:
	lsl r1, r5, #3					// Stellen fuer die Verschiebung berechnen
	lsr r0, r4, r1 					// zu uebertragendes Byte (MSB first) an die korrekte (unterste) Stelle schieben
	strb r0, [r6]					// ins I2C-Datenregister schreiben 
	bl i2c_Wait_Complete			// warten, bis die Uebertragung beendet ist
	cbnz r0, ds75_write_err			// R0 = 1? => Fehler nach oben reichen
	subs r5, r5, #1					// Parameter-Zaehler dekrementieren
	bpl ds75_write_loop				// wiederholen, falls noch nicht alle Daten uebertragen wurden
	bl i2c_Stop						// I2C-Bus-Stop
	mov r0, #0						// Kein Fehler aufgetreten
ds75_write_err:
	pop {r4-r6,pc}

/*
 * I2C-Bus: wartet, bis ein Byte uebertragen wurde
 * (TCF-Flag ist nicht fehlerfrei benutzbar!)
 * Parameter: keine
 * Rueckgabe: r0: 0 = kein Fehler, 1 = Fehler
 */	
.thumb_func
i2c_Wait_Complete:
	push {lr}
	ldr r3, =I2C0_S_IICIF			// IICIF-Bit (Bit-banding)
	mov r2, #WAIT_IICIF				// Versuchszaehler (an I2C-Taktfrequenz angepasst)
	mov r0, #0						// Fehler-Flag zuruecksetzen
i2c_WC_Loop:
	ldrb r1, [r3]					// IICIF-Bit auslesen; Registerbreite auch bei Bit-banding beachten!
	cbnz r1, i2c_WC_OK				// Bit ist gesetzt, also alles in Ordnung
	subs r2, r2 , #1				// Versuchszaehler dekrementieren
	bne i2c_WC_Loop					// weiterer Versuch, bis Versuchszaehler auf 0
	
	ldr r0, =i2c_iicif_err_str		// Fehlermeldung 
	bl uart_putString				// ausgeben
	mov r0, #1						// Fehlerflag setzen (Achtung: in diesem Loesungsvorschlag wird der Fehler nicht weiter ueberprueft!)
									// Man kann die I2C-Fehlerpruefung recht aufwendig gestalten, aber das wuerde den Umfang des Versuchs sprengen.
	mov r1, #1
	ldr r2, =I2C0_S_ARBL
	strb r1, [r2]					// ARBL zuruecksetzen
	ldr r3, =I2C0_S_IICIF			// IICIF-Bit (Im Fehlerfall neu laden nach Terminalausgabe)
i2c_WC_OK:
	strb r1, [r3]					// IICIF-Bit loeschen, indem eine Eins geschrieben wird
	pop {pc}

/*
 * Gibt die gemessene Temperatur auf dem Terminal aus
 * Parameter: r0 = Temperatur (0x00VVN000, FP: 16.16)
 * Rueckgabe: keine
 */		
.thumb_func
print_Temp:
	push {r4,lr}
	mov r4, r0						// Temperatur sichern
	ldr r0, =str_0x					// "0x" ausgeben
	bl uart_putString
	mov r0, r4
	bl uart_putInt32				// Temperatur ausgeben
	ldr r0, =str_separator			// Separator
	bl uart_putString				// ausgeben
	lsr r0, r4, #16					// Vorkommastellen
	bl uart_putByteBase10			// ausgeben

	ubfx r2, r4, #12, #4			// Nachkommastellen separieren

	ldr r1, =str_table				// Startadresse der Stringtabelle
	ldr r0, [r1, r2, lsl #2]		// Stringadresse mit Index laden 
	bl uart_putString				// und ausgeben
	
	ldr r0, =str_celsius			// "Grad Celsius"
	bl uart_putString				// ausgeben
	pop {r4,pc}	

/*** Datenbereich (ab 0x20000000) ***/
.data
timer_flag: .word 0x0

// Tabelle der Adressen der Strings fuer die Nachkommastellen
str_table: .word str_0, str_1, str_2, str_3, str_4, str_5, str_6, str_7, str_8, str_9, str_a, str_b, str_c, str_d, str_e, str_f

str_0x: .asciz "0x"
str_separator: .asciz ": "
str_celsius: .asciz " Grad Celsius\n"

i2c_gpio_err_str: .asciz "I2C-Bus-Fehler (GPIO)! Programm angehalten!\n"
i2c_iicif_err_str: .asciz "Uebertragung wurde nicht korrekt (kein IICIF) beendet!\n"
i2c_mst_err_str: .asciz "I2C-Master-Mode nicht moeglich!\n"

// Liste der Strings fuer die Nachkommastellen
str_0: .asciz ",0" 
str_1: .asciz ",0625" 
str_2: .asciz ",125" 
str_3: .asciz ",1875" 
str_4: .asciz ",25" 
str_5: .asciz ",3125" 
str_6: .asciz ",375" 
str_7: .asciz ",4375"
str_8: .asciz ",5"
str_9: .asciz ",5625"
str_a: .asciz ",625" 
str_b: .asciz ",6875"
str_c: .asciz ",75"
str_d: .asciz ",8125"
str_e: .asciz ",875"
str_f: .asciz ",9375"

/************************************************************
Versuch: 5-1
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"				// Praktikumsspezifische Definitionen
.include "lib_pit.inc"				// Einfache Unterprogramme zur Zeitmessung
.include "lib_uart.inc"				// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text								// hier beginnt ein Code-Segment
.align	2							// Ausrichtung an eine gerade Adresse
.global	main						// "main" wird als globales Symbol deklariert
.syntax unified

// Registeradressen
.equ SIM_SCGC3,     0x40048030
.equ SIM_SCGC5,     0x40048038
.equ SIM_SCGC6,     0x4004803C

.equ PORTA_PCR19,   0x4004904C
.equ PORTE_PCR26,   0x4004d068

.equ PORTD_PCR, 	0x4004C000		// Basisadresse der Port D PCR-Register
.equ PCR12, 		12<<2			// SPI2-CLK, Offset = Port-Nr. * 4 
.equ PCR13, 		13<<2			// SPI2-MOSI
.equ PCR14, 		14<<2			// SPI2-MISO
.equ PCR15, 		15<<2			// SPI2-CS1

.equ GPIOA_PDDR,    0x400FF014
.equ GPIOE_PDDR,    0x400FF114

.equ PIT_MCR,       0x40037000
.equ PIT_LDVAL1,    0x40037110
.equ PIT_TCTRL1,    0x40037118
.equ PIT_TFLG1,     0x4003711C

.equ SPI2_MCR, 		0x400AC000
.equ SPI2_CTAR0, 	0x400AC00C
.equ SPI2_SR, 		0x400AC02C
.equ SPI2_RSER,		0x400AC030
.equ SPI2_PUSHR, 	0x400AC034
.equ SPI2_POPR, 	0x400AC038

.equ NVIC_ISER2, 	0xE000E108
.equ NVIC_ICPR2, 	0xE000E288
.equ SCR_VTOR,		0xE000ED08		// Vector Table Offset Register (enthaelt Basisadresse der Interrupt-Vektor-Tabelle)

// Bit Band-Konstanten
__BBREG	BB_SIM_SPI2, SIM_SCGC3, 12
__BBREG	BB_SIM_PIT, SIM_SCGC6, 23

__BBREG	BB_PDDR_SW1, GPIOA_PDDR, 19
__BBREG	BB_PDDR_SW2, GPIOE_PDDR, 26
__BBREG	BB_SW1_CIF, PORTA_PCR19, 24
__BBREG	BB_SW2_CIF, PORTE_PCR26, 24
__BBREG	BB_PIT1_TEN, PIT_TCTRL1, 0

__BBREG	SPI2_SR_TCF, SPI2_SR, 31	// BB-Alias zum TC Flag
__BBREG SPI2_MCR_MDIS, SPI2_MCR, 14	// BB-Alias zum MDIS-Bit

// weitere Konstanten
.equ PORT_MASK_SIM, 0x3200			// zu aktivierende PORTS (A, D, E) in SCGC5
.equ NVIC_ISER2_MASK, 0x8800020 	// Bit 27 (Port E), Bit 23 (Port A), 5 (PIT1)

.equ IVT_OFS_PIT1, 0x0154			// IVT-Offsets der ISRs
.equ IVT_OFS_PTA, 0x019C		
.equ IVT_OFS_PTE, 0x01AC

.equ PIT1_LD1200, 30000000-1  		// 1,2 Sekunden Verzoegerung fuer Standardaufloesung 
.equ PIT1_LD0600, 15000000-1 
.equ PIT1_LD0300,  7500000-1
.equ PIT1_LD0150,  3750000-1
.equ PIT1_LD0075,  1875000-1

.equ PIT_TCTRL_VAL, 3				// nicht mit PIT0 verkettet, Interrupt aktiviert, Timer aktiviert

.equ PCR_ALT2, 0x200				// Alternative 2 (SPI)

.equ GPIO_IRQ_PE_SET, 0xA0103		// GPIO-Modus, IRQ (fallende Flanke), Pull-Up-Widerstand

.equ LF, 0x0a
.equ NK_STRLEN, 6					// Stringlaenge fuer Nachkommastellen inkl. Null-Terminator

.equ MCR_CFG1, 0x81003C00 			// Master Mode, no cont. SCK, SPI Module enabled, Transfer in Debug Mode enabled, 
									// Modified transfer format disabled, Overflow Data is shifted, inactive PCSx is low,
									// no Doze Mode Support, Module clocks enabled, FIFOs disabled, start transfers				

.equ CTAR0_CFG1, 0x7A880107 		// 16-Bit Frames (DS1722-Manual: S. 8, Figure 4/5, CPOL=0, CPHA=1, Baudrate ca. 100kHz
									// Wichtige einzuhaltende Zeiten (DS1722-Manual: S. 12): t_CC, t_CCH, t_CWH
									// t_CC  (>= 400 ns) = t_CSC = 40 ns * PCSSCK(5) * CSSCK(2) = 400 ns -> PCSSCK: 0b10, CSSCK: 0b0000
									// t_CCH (>= 100 ns) = t_ASC = 40 ns * PASC(1) * ASC(4) = 160 ns -> PASC: 0b00, ASC: 0b0001
									// t_CWH (>= 400 ns) = t_DT  = 40 ns * PDT(5) * DT(2) = 400 ns -> PDT: 0b10, DT: 0b0000     

.equ DS1722_CONFIG, 0xE8			// Standardaufloesung auf 12 Bit, kontinuierliche Messung aktivieren
.equ DS_LSB, 1						// Ansteuerung von LSB und MSB
.equ DS_MSB, 2
.equ SPI_PCS1, (1<<17)				// PCS1-Bit: 0x20000
.equ ADR_80, 0x80					// Initiale Adresse mit gesetztem Schreibbit
.equ DS_CMDW, SPI_PCS1 | ADR_80 << 8// Schreib-Kommando
.equ IRES, 12						// Initiale Aufloesung
.equ ONESHOT, 0xE0					// Konfiguration: 1 1 1 1SHOT=0

/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind.
*/
main:
	bl uart_init

	mov r10, #0
	mov r11, #1

// Modul-Initialisierung
	ldr r1, =BB_SIM_SPI2			// Takt fuer SPI2 aktivieren
	str r11, [r1]
	ldr r1, =BB_SIM_PIT				// Takt fuer PIT aktivieren
	str r11, [r1]
	ldr r0, =SIM_SCGC5				// Takt fuer die PORTS aktivieren
	ldr r1, [r0]
	orr r1, r1, #PORT_MASK_SIM
	str r1, [r0]
	
// NVIC-Bitmaske fuer PORT_E, PORT_A, PIT1 setzen
	ldr r0, =NVIC_ICPR2 
	ldr r1, =NVIC_ISER2_MASK
	str r1, [r0]
	ldr r0, =NVIC_ISER2 
	str r1, [r0]
	
// Adressen der ISRs fuer PIT1, Port A und Port E in die IVT eintragen
	ldr r1, =SCR_VTOR
	ldr r0, [r1]
	ldr r1, =isr_pit1
	str r1, [r0, #IVT_OFS_PIT1]
	ldr r1, =isr_port_a
	str r1, [r0, #IVT_OFS_PTA]
	ldr r1, =isr_port_e
	str r1, [r0, #IVT_OFS_PTE]
	
// Taster konfigurieren
	ldr r1, =GPIO_IRQ_PE_SET 		// GPIO-Modus mit IRQ (fallende Flanke) und Pull-Up-Widerstand 	
	ldr r0, =PORTA_PCR19 			// fuer SW1
	str r1, [r0]					// festlegen
	ldr r0, =PORTE_PCR26 			// fuer SW2
	str r1, [r0]					// festlegen

// Eingaenge (SW1 und SW2)
	ldr r0, =BB_PDDR_SW1
	str r10, [r0]
	ldr r0, =BB_PDDR_SW2
	str r10, [r0]
	
// PORTD_PCR (SPI2) konfigurieren
	mov r0, #PCR_ALT2				// Alternative 2 (SPI2) fuer die benutzten Port D Pins waehlen; PCR12 bis PCR15
	ldr r1, =PORTD_PCR				// Basisadresse der Port D PCR-Register
	str r0, [r1, #PCR12]
	str r0, [r1, #PCR13]
	str r0, [r1, #PCR14]
	str r0, [r1, #PCR15]
	
	ldr r1, =SPI2_RSER				// Alle Interrupts ausschalten
	str r10, [r1]
	
	ldr r1, =SPI2_CTAR0				// Baudrate und Transferparameter konfigurieren
	ldr r0, =CTAR0_CFG1				 
	str r0, [r1]
	
	ldr r1, =SPI2_MCR_MDIS			// Clear MCR_MDIS, um FIFOs deaktivieren zu koennen
	str r10, [r1]					
	ldr r1, =SPI2_MCR				// Modul konfigurieren
	ldr r0, =MCR_CFG1				// Master Mode aktivieren, FIFOs deaktivieren, HALT deaktivieren
	str r0, [r1]

// DS1722 konfigurieren
	mov r0, #DS1722_CONFIG	
	bl spi_writeConfig
					
	ldr r0, =str_res				// Ausgabe der Standardaufloesung
	bl uart_putString
	mov r0, #IRES					// Initiale Aufloesung ausgeben 
	bl uart_putByteBase10
	ldr r0, =str_bit
	bl uart_putString
	
// PIT-Modul konfigurieren
	ldr r0, =PIT_MCR
	str r11, [r0]					// R11=1 -> MDIS=0, FRZ=1 (PIT-Clock enabled, PIT stopped in Debug mode) 
	
// PIT1 fuer Wandlungszeit der Standardaufloesung einstellen
	ldr r12, =PIT_LDVAL1
	ldr r1, =PIT1_LD1200 
	str r1, [r12]
	
// PIT1 aktivieren
	ldr r0, =PIT_TCTRL1
	mov r1, #PIT_TCTRL_VAL
	str r1, [r0]

// haeufig verwendete Adressen vorhalten
	ldr r5, =pit_flag
	ldr r6, =res_changed
	ldr r7, =ds1722_res
	ldr r8, =ds1722_time
	ldr r9, =BB_PIT1_TEN

// R10 = 0, R11 = 1, R12 = PIT_LDVAL1
	 	
main_loop:		
	ldr r0, [r5]					// PIT-Flag laden			
	cmp r0, #1						// minimale Wandlungszeit abgelaufen?
	bne main_loop
	str r10, [r5]					// PIT-Flag zuruecksetzen

	mov r0, #DS_LSB					// Temperatur LSB ueber SPI lesen
	bl spi_readByte
	mov r4, r0						// LSB sichern
	
	mov r0, #DS_MSB					// Temperatur MSB ueber SPI lesen
	bl spi_readByte

	mov r1, r4						// LSB restaurieren
	bl put_Temp						// Ausgabe der Temperatur auf dem Terminal

	ldr r0, [r6]					// Flag fuer neue Konfiguration laden
	cmp r0, #1
	bne main_loop					// wenn Flag = 0, dann Konfiguration unveraendert => nur auf PIT warten

// Konfiguration aendern
	str r10, [r9]					// PIT deaktivieren, damit er nicht waehrend der Konfigurationsaenderung ausloesen kann

	ldr r4, [r7]					// Nr. der geaenderten Konfiguration laden
	
	ldr r0, =str_res				// Ausgabe der neuen Aufloesung
	bl uart_putString
	add r0, r4, #8					// 8..12 Bit (Index + 8)
	bl uart_putByteBase10
	ldr r0, =str_bit
	bl uart_putString
	
// DS1722 neu konfigurieren
// Konfigurationsdaten zur Aufloesung  (1 1 1 1SHOT R2 R1 R0 SD), 1SHOT = 0, SD = 0
	orr r0, r4, #(ONESHOT >> 1)		// statische obere Haelfte (1 1 1 1SHOT) verschoben (Stellenanpassung) mit Index verodern
	lsl r0, r0, #1					// Verschiebung korrigieren und SD = 0 setzen
	bl spi_writeConfig				// DS1722-Konfiguration aendern
	
// Wandlungszeit anpassen
	ldr r0, [r8, r4, lsl #2]		// benoetigte Wandlungszeit der neuen Aufloesung aus Tabelle holen
	str r0, [r12]					// und in LDVAL eintragen

	str r11, [r9]					// PIT starten
	
	str r10, [r6]					// Flag fuer neue Konfiguration zuruecksetzen

	b main_loop

/*** Unterprogramme und ISR ***/

/*
 * Wartet, bis das Transfer Complete Flag gesetzt wurde und setzt es zurueck
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
spi_waitForTCF:
	ldr r1, =SPI2_SR_TCF
loop_tcf:	
	ldr r0, [r1]					// TCF pruefen
	cmp r0, #1
	bne loop_tcf
	str r0, [r1]					// TCF loeschen
	bx lr

/*
 * Schreibt eine Konfiguration ueber SPI ins DS1722
 * Parameter: r0 = Konfiguration
 * Rueckgabe: keine
 */		
.thumb_func
spi_writeConfig:
	push {lr}
	ldr r1, =SPI2_PUSHR
	orr r0, r0, DS_CMDW				// Konfiguration mit Schreib-Kommando verknuepfen
	str r0, [r1]					
	bl spi_waitForTCF

	ldr r1, =SPI2_POPR				// Schieberegister leeren
	ldr r0, [r1]			
	
	pop {pc}

/*
 * Liest ein Byte ueber SPI ein
 * Parameter: r0 = Adresse
 * Rueckgabe: r0 = gelesenes Byte
 */		
.thumb_func
spi_readByte:
	push {lr}
	ldr r1, =SPI2_PUSHR
	ldr r2, =SPI_PCS1				// PCS1 aktiv
	orr r0, r2, r0, lsl #8			// Adresse verschieben und hinzufuegen
	str r0, [r1]
	bl spi_waitForTCF
	ldr r1, =SPI2_POPR				// Wert einlesen
	ldr r0, [r1]			
	and r0, r0, #0xFF				// Rueckgabewert auf 8 bit begrenzen
	pop {pc}

/*
 * Gibt die Temperatur in der Form "dd,dddd gC" aus.
 * Eine eigene Tabelle mit den String-Adressen ist nicht notwendig, da das Format vier Nachkommastellen fest vorgibt.
 * Parameter: r0 = MSB, r1 = LSB
 * Rueckgabe: keine
 */		
.thumb_func
put_Temp:
	push {r4,lr}
	
	mov r4, r1						// LSB sichern
	
	bl uart_putByteBase10			// Temperaturauswertung von 0-48 gC, MSB kann daher vorzeichenlos interpretiert werden.

	lsr r0, r4, #4					// Nachkommastellen an die richtige Position schieben
	mov r1, #NK_STRLEN				// Stringlaenge inkl. Null-Terminator
	ldr r2, =str_nk
	mla r0, r0, r1, r2				// Adressoffset berechnen und Startadresse der Stringtabelle addieren
	bl uart_putString				// ausgeben
	
	ldr r0, =str_einheit			// Einheit und neue Zeile ausgeben
	bl uart_putString
	
	pop {r4,pc}

/*
 * ISR fuer PIT1 (Wandlungszeit abwarten)
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
isr_pit1:							// ISR fuer PIT1
	mov r0, #1					
	ldr r1, =pit_flag				// Flag fuer PIT
	str r0, [r1]					// setzen
	ldr r1, =PIT_TFLG1				// Interrupt-Flag
	str r0, [r1] 					// zuruecksetzen
	bx lr							// Ruecksprung

/*
 * ISR fuer SW1 (Port A)
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
isr_port_a: 						 
	mov r1, #1					
	ldr r0, =BB_SW1_CIF				// Bit-Band-Adresse des Interrupt-Flags fuer SW1
	str r1, [r0]					// Flag loeschen
	ldr r0, =res_changed			// Aufloesung
	str r1, [r0]					// geaendert
	ldr r0, =ds1722_res				// aktuelle Aufloesung
	ldr r1, [r0]					// laden
	add r1, r1, #1					// erhoehen
	cmp r1, #4						// Ueberlauf ?
	it hi							// wenn ja,
	movhi r1, #0					// dann auf 0 setzen
	str r1, [r0] 					// neue Ausfloesung zurueckschreiben
	bx lr

/*
 * ISR fuer SW2 (Port E)
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
isr_port_e: 						
	mov r1, #1					
	ldr r0, =BB_SW2_CIF				// Bit-Band-Adresse des Interrupt-Flags fuer SW2
	str r1, [r0]					// Flag loeschen
	ldr r0, =res_changed			// Aufloesung
	str r1, [r0]					// geaendert
	ldr r0, =ds1722_res				// aktuelle Aufloesung
	ldr r1, [r0]					// laden
	subs r1, r1, #1					// verkleinern
	it mi							// wenn Unterlauf,
	movmi r1, #4					// dann auf 4 (Maximum) setzen
	str r1, [r0] 					// neue Ausfloesung zurueckschreiben
	bx lr

/*** Datenbereich (ab 0x20000000) ***/
.data
ds1722_time:						// Konfigurationsdaten zur Wandlungsdauer
.word PIT1_LD0075					// 0.075 s
.word PIT1_LD0150					// 0.15 s
.word PIT1_LD0300					// 0.3 s
.word PIT1_LD0600					// 0.6 s
.word PIT1_LD1200					// 1.2 s

pit_flag: .word 0
ds1722_res: .word IRES - 8			// Index der Standardaufloesung (= Aufloesung - 8)
res_changed: .word 0				// Flag zur Anzeige einer Aufloesungsaenderung

str_res: .asciz "Aufloesung: "
str_bit: .asciz " Bit\n"
str_nk: .asciz ",0000", ",0625", ",1250", ",1875", ",2500", ",3125", ",3750", ",4375", ",5000", ",5625", ",6250", ",6875", ",7500", ",8125", ",8750", ",9375"
str_einheit: .asciz " gC\n"

/************************************************************
Versuch: 6-1
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"				// Praktikumsspezifische Definitionen
.include "lib_uart.inc"				// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text								// hier beginnt ein Code-Segment
.align	2							// Ausrichtung an eine gerade Adresse
.global	main						// "main" wird als globales Symbol deklariert
.syntax unified

// Registeradressen
.equ SIM_SCGC6,	0x4004803c

.equ NVIC_ISER2, 0xe000e108
.equ NVIC_ICPR2, 0xe000e288

// Offset-Adressierung
.equ RTC_BASE,	0x4003d000			// RTC-Basisadresse (RTC_TSR)
.equ O_RTC_TSR, 0x0
.equ O_RTC_TPR, 0x4
.equ O_RTC_TAR, 0x8
.equ O_RTC_CR,  0x10
.equ O_RTC_SR,  0x14
.equ O_RTC_IER, 0x1c

// Bit Band-Konstanten
__BBREG BB_SIM_RTC, SIM_SCGC6, 29

__BBREG RTC_SR_TCE, RTC_BASE + O_RTC_SR, 4	// Time Count Enable
__BBREG RTC_CR_SWR, RTC_BASE + O_RTC_CR, 0	// Software Reset
__BBREG RTC_CR_OSCE, RTC_BASE + O_RTC_CR, 8	// 32 kHz Oscillator Enable

// weitere Konstanten
.equ NVIC_RTC_AL,	(1 << (66 % 32))
.equ NVIC_RTC_SEC,	(1 << (67 % 32))

.equ SCR_VTOR, 0xE000ED08			// Vector Table Offset Register (enthaelt Basisadresse der Interrupt-Vektor-Tabelle)
.equ IVT_RTC_AL_OFS, 0x148			// Offset fuer Alarm Interrupt (RTC)
.equ IVT_RTC_SEC_OFS, 0x14c			// Offset fuer Sekunden Interrupt (RTC)

.equ BIT_TAIE, 0x04					// Time Alarm Interrupt Enable (RTC_IER)
.equ BIT_TSIE, 0x10					// Time Seconds Interrupt Enable (RTC_IER)

.equ OSC_DELAY, 8333335				// Zaehler fuer Einschwingzeit des 32-kHz-Oszillators (min. 1000 ms)
									// K60 Sub-Family Data Sheet, Abschnitt 6.3.3.2, S. 33
.equ SECS_DAY,	3600 * 24
.equ SECS_HOUR,	3600
.equ SECS_MIN,	60
.equ AL_DAY,	SECS_DAY - 1		// Alarm in Sekunden
.equ AL_HOUR,	SECS_HOUR - 1

.equ DUO_ASC,	0x3030				// Zur doppelten Umwandlung in ASCII-Code

.equ LF, 0x0a						// Neue Zeile

/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind.
*/
main:	
// Initialisiere UART 
	bl uart_init

	mov r10, #0
	mov r11, #1

// RTC Modul aktivieren	
	ldr r0, =BB_SIM_RTC
	str r11, [r0]
	
// ISRs in IVT eintragen
	ldr r1, =SCR_VTOR
	ldr r0, [r1]
	ldr r1, =isr_rtc_al
	str r1, [r0, #IVT_RTC_AL_OFS]
	ldr r1, =isr_rtc_sec
	str r1, [r0, #IVT_RTC_SEC_OFS]

// RTC Software Reset
	ldr r0, =RTC_CR_SWR
	str r11, [r0]
	str r10, [r0]
	
/* R/W Access Control Register
 * Diese Register werden vom Software Reset nicht geaendert!
 * Sollte eines der Bits in diesen Registern 0 sein, laesst es sich danach nicht mehr auf 1 setzen!
 * Es wird erst nach dem naechsten POR (Power On Reset) automatisch wieder gesetzt.
 * Das Setzen von Bits in diesen beiden Registern ist daher zwecklos.
 
.equ O_RTC_WAR, 0x800
.equ O_RTC_RAR, 0x804

	ldr r9, =RTC_BASE
	mov r0, #0xe
	str r0, [r9, #O_RTC_RAR]	// Bit #0 auf Null setzen
	nop
	mov r0, #0xf
	str r0, [r9, #O_RTC_RAR]	// wird ignoriert!
	nop
*/
	
// Nur Sekunden- und Alarm-Interrupt aktivieren
// (nach Reset sind alle bis auf TSIE aktiviert!)
// Daher duerfen die RTC-Interrupts im NVIC erst spaeter aktiviert werden! 
	ldr r9, =RTC_BASE
	mov r1, #(BIT_TSIE | BIT_TAIE)	// Sekunden- und Alarm-Interrupt aktivieren
	str r1, [r9, #O_RTC_IER]	
		
// 32-kHz-Oszillator aktivieren...
	ldr r0, =RTC_CR_OSCE
	str r11, [r0]

// Terminalausgabe
	ldr r0, =txt_start
	bl uart_putString
	
// ... und einschwingen lassen
	ldr r0, =OSC_DELAY
rtc_osc_wait:
	subs r0, r0, #1
	bne rtc_osc_wait

// Terminalausgabe
	ldr r0, =txt_end
	bl uart_putString
		
//  RTC-Interrupts im NVIC zulassen
	mov r1, #(NVIC_RTC_AL | NVIC_RTC_SEC)
	ldr r0, =NVIC_ICPR2
	str r1, [r0] 
	ldr r0, =NVIC_ISER2
	str r1, [r0]

// Register vorbelegen
	ldr r4, =al_flag
	ldr r5, =sec_flag
	mov r6, #1						// Show_Flag: 0: Anzeige aus, 1: Anzeige an
	mov r7, #1						// DH_Flag: 0: Stunden-Modus, 1: Tages-Modus
	ldr r8, =RTC_SR_TCE
//	R9: RTC_BASE (s. o.)
//	R10: #0
//	R11: #1

// TSR (TIF zuruecksetzen) und TAR initialisieren
	str r10, [r9, #O_RTC_TSR]
	ldr r0, =AL_DAY					// Alarm festlegen
	str r0, [r9, #O_RTC_TAR]

// zuletzt den Sekundenzaehler aktivieren
	str r11, [r8]
	
mainloop:
	ldr r0, [r4]					// Alarm-Flag pruefen
	cbz r0, no_alarm
	str r10, [r4]					// Alarm-Flag zuruecksetzen
	ldr r0, =txt_al					// Alarm-Text ausgeben
	bl uart_putString
	
no_alarm:
	ldr r0, [r5]					// Sekunden-Flag pruefen
	cbz r0, no_sec
	str r10, [r5]					// Sekunden-Flag zuruecksetzen
	
	cmp r6, #0						// Wenn Show_Flag=0, dann nichts ausgeben
	beq mainloop

	ldr r0, [r9, #O_RTC_TSR]		// Sekunden auslesen
	bl calc_time					// Zeitangabe umrechnen und eintragen

	cmp r7, #0						// 24-/1-Stunden-Modus?
	ite eq
	ldreq r0, =(txt_time + 3)		// ohne Stundenausgabe
	ldrne r0, =txt_time				// mit Stundenausgabe
	bl uart_putString
	
no_sec:	         
	bl uart_charPresent
	cmp r0, #0
	beq mainloop
	
// Eingabe verarbeiten	
	bl uart_getChar
	cmp r0, #'d'
	beq set_day						// Tag-Modus aktivieren
	cmp r0, #'h'
	beq set_hour					// Stunden-Modus aktivieren
	cmp r0, #'r'
	beq reset_rtc					// Reset
	cmp r0, #'s'
	beq start_stop					// Anzeige an/aus
	cmp r0, #'p'
	beq preset						// Preset einstellen
	b mainloop
	
set_day:
	mov r7, #1						// 24-Stunden-Modus
	ldr r0, =txt_24h				// Textausgabe vorbereiten
	ldr r1, =AL_DAY					// Alarmzeit vogeben
	bl set_rtc
	b mainloop
	
set_hour:
	mov r7, #0						// 1-Stunden-Modus
	ldr r0, =txt_1h					// Textausgabe vorbereiten
	ldr r1, =AL_HOUR				// Alarmzeit vorgeben
	bl set_rtc
	b mainloop
	
reset_rtc:
	ldr r0, =txt_reset				// Textausgabe vorbereiten
	ldr r1, [r9, #O_RTC_TAR]		// Alarmzeitpunkt auslesen
	bl set_rtc
	b mainloop
	
start_stop:
	eors r6, r6, #1					// Show-Flag invertieren
	ite eq							// entsprechenden Text selektieren
	ldreq r0, =txt_off
	ldrne r0, =txt_on
	bl uart_putString				// und ausgeben
	b mainloop
	
preset:
	cmp r7, #0						// 24-/1-Stunden-Modus abfragen
	ite eq
	ldreq r0, =(SECS_HOUR - 5)		// entsprechenden Sekundenwert laden
	ldrne r0, =(SECS_DAY - 5)
	str r10, [r8]					// RTC anhalten
	str r10, [r9, #O_RTC_TPR]		// Prescaler vor TSR zuruecksetzen!
	str r0, [r9, #O_RTC_TSR]		// TSR auf gewaehlten Sekundenwert einstellen
	str r11, [r8]					// RTC starten
	ldr r0, =txt_preset
	bl uart_putString
	b mainloop

/*** Unterprogramme und ISR ***/

/*
 * Berechnet aus dem uebergebenen Sekundenwert die einzelnen Ziffern der Uhrzeit und 
 * traegt diese an den entsprechenden Stellen in den String "txt_time" ein. 
 * Parameter: r0: Zeit in Sekunden
 * Rueckgabe: keine
 */		
.thumb_func
calc_time:
	push {r4,r5}
	ldr r5, =DUO_ASC				// Wert für doppelte Umwandlung				
	ldr r4, =txt_time				// Zeiger auf String
	mov r3, #10						// Multiplikator
	
	ldr r2, =SECS_DAY			
	udiv r1, r0, r2					// Tagesueberlauf berechnen und subtrahieren
	mls r0, r1, r2, r0				// Restsekunden

	mov r2, #SECS_HOUR				// Sekunden pro Stunde
	udiv r1, r0, r2					// Stunden
	mls r0, r1, r2, r0				// Restsekunden
	udiv r2, r1, r3					// Zehner
	mls r1, r2, r3, r1				// Einer
	bfi r2, r1, #8, #4				// Einer in Byte #1 von R2 verschieben 
	uadd8 r2, r2, r5				// Beide Ziffern in ASCII-Code umwandeln		
	strh r2, [r4]					// In String eintragen (Little Endian beachten)

	mov r2, #SECS_MIN				// Sekunden pro Minuten
	udiv r1, r0, r2					// Minuten
	mls r0, r1, r2, r0				// Restsekunden
	udiv r2, r1, r3					// Zehner
	mls r1, r2, r3, r1				// Einer
	bfi r2, r1, #8, #4				// Einer in Byte #1 von R2 verschieben 
	uadd8 r2, r2, r5				// Beide Ziffern in ASCII-Code umwandeln		
	strh r2, [r4, #3]				// In String eintragen (Little Endian beachten)

	udiv r2, r0, r3					// Zehner (Sekunden)
	mls r1, r2, r3, r0				// Einer
	bfi r2, r1, #8, #4				// Einer in Byte #1 von R2 verschieben 
	uadd8 r2, r2, r5				// Beide Ziffern in ASCII-Code umwandeln		
	strh r2, [r4, #6]				// In String eintragen (Little Endian beachten)

	pop {r4,r5}
	bx lr	
	
/*
 * Setzt die Parameter für den 24-/1-Stunden-Modus
 * Parameter: r0: Ausgabetext, r1: Alarmwert 
 * Rueckgabe: keine
 */		
.thumb_func
set_rtc:
	push {lr}
	ldr r2, =RTC_BASE
	str r1, [r2, #O_RTC_TAR]		// Alarm einstellen
	ldr r1, =RTC_SR_TCE
	mov r3, #0
	str r3, [r1]					// RTC anhalten
	str r3, [r2, #O_RTC_TPR]		// Prescaler vor TSR zuruecksetzen
	str r3, [r2, #O_RTC_TSR]		// Sekunden auf Null setzen (Reset TIF)
	mov r3, #1
	str r3, [r1]					// RTC starten
	bl uart_putString
	pop {pc}	

/*
 * ISR fuer den Alarm-IRQ
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
isr_rtc_al:
	ldr r1, =RTC_BASE
	ldr r0, [r1, #O_RTC_TAR]		// Alarmzeit beibehalten
	str r0, [r1, #O_RTC_TAR]
	ldr r1, =al_flag				// Alarm-Flag setzen
	mov r0, #1
	str r0, [r1]
	bx lr 

/*
 * ISR fuer den Sekunden-IRQ
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
isr_rtc_sec:
	ldr r1, =sec_flag				// Sekunden-Flag setzen
	mov r0, #1
	str r0, [r1]
	bx lr 

/*** Datenbereich (ab 0x20000000) ***/
.data
sec_flag: 	.word 0
al_flag: 	.word 0
txt_24h: 	.asciz "24-Stunden-Modus\n"
txt_1h: 	.asciz "1-Stunden-Modus\n"
txt_reset: 	.asciz "Uhr zurueckgesetzt\n"
txt_on: 	.asciz "Anzeige an\n"
txt_off: 	.asciz "Anzeige aus\n"
txt_preset: .asciz "Uhr vorgestellt\n"
txt_al: 	.asciz "Endzeitpunkt erreicht\n"
txt_time: 	.asciz "00:00:00 Uhr\n"
txt_start:	.asciz "\n32 kHz Oszillator einschwingen lassen (1 s)... "
txt_end:	.asciz "fertig.\n"

/************************************************************
Versuch: 6-2
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"				// Praktikumsspezifische Definitionen
.include "lib_uart.inc"				// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text								// hier beginnt ein Code-Segment
.align	2							// Ausrichtung an eine gerade Adresse
.global	main						// "main" wird als globales Symbol deklariert
.syntax unified

// Registeradressen
.equ SIM_SCGC6, 	0x4004803C
.equ SIM_SCGC5, 	0x40048038

.equ SCR_VTOR, 		0xE000ED08		// Vector Table Offset Register (enthaelt Basisadresse der Interrupt-Vektor-Tabelle)
.equ NVIC_ISER2,	0xE000E108
.equ NVIC_ICPR2,	0xE000E288

.equ PIT_MCR,       0x40037000
.equ PIT_LDVAL0,    0x40037100
.equ PIT_TCTRL0,    0x40037108
.equ PIT_TFLG0, 	0x4003710C

.equ PORTA_PCR6, 	0x40049018		// DCF77-Signal
.equ PORTA_PCR10,	0x40049028		// LED (blau)
.equ GPIOA_PDOR, 	0x400FF000
.equ GPIOA_PDDR, 	0x400FF014

.equ RTC_BASE, 		0x4003d000		// RTC-Basisadresse (RTC_TSR)
.equ RTC_CR,		0x4003d010		// Control
.equ RTC_SR,		0x4003d014		// Status

// Register-Offsets zur Basisadresse
.equ O_RTC_TSR, 0x0
.equ O_RTC_TPR, 0x4
.equ O_RTC_IER, 0x1c

// Bit-band Konstanten
__BBREG SIM_SCGC5_PORTA, SIM_SCGC5, 9
__BBREG SIM_SCGC6_PIT, SIM_SCGC6, 23
__BBREG SIM_SCGC6_RTC, SIM_SCGC6, 29

__BBREG DCF77_PDDR, GPIOA_PDDR, 6
__BBREG DCF77_PDOR, GPIOA_PDOR, 6
__BBREG LED_PDDR, GPIOA_PDDR, 10
__BBREG LED_PDOR, GPIOA_PDOR, 10

__BBREG RTC_CR_OSCE, RTC_CR, 8		// 32 kHz Oszillator Enable
__BBREG RTC_CR_SWR, RTC_CR, 0 		// Software Reset
__BBREG RTC_SR_TCE, RTC_SR, 4		// Time Count Enable

// weitere Konstanten
.equ NVIC_RTC_SEC, (1 << (67 % 32))	// Bitposition für RTC-Sekunden-IRQ im NVIC
.equ IVT_RTC_SEC_OFS, 0x14C			// IVT-Offset fuer Sekunden Interrupt (RTC)
.equ NVIC_PIT0, (1 << (68 % 32))	// Bitposition für PIT0 im NVIC
.equ IVT_PIT0_OFS, 0x150			// IVT-Offset fuer PIT0

.equ BIT_TSIE, 0x10					// Time Seconds Interrupt Enable (RTC_IER)

.equ PCR_GPIO, 0x100				// MUX = GPIO

.equ PIT_START, 3					// PIT=aktiviert, IRQ=aktviert, CHN=nicht verkettet

.equ OSC_DELAY, 8333335				// Zaehler fuer Einschwingzeit des 32-kHz-Oszillators (min. 1000 ms)
									// K60 Family Reference Manual, Abschnitt 6.3.3.2, S. 33
.equ TAG_SEKUNDEN,	24 * 3600
.equ STUNDEN_MINUTEN,	60
.equ STUNDEN_SEKUNDEN,	3600
.equ MINUTEN_SEKUNDEN,	60
.equ WOCHENTAGE,	7
.equ MAX_MONATE,	12
.equ MAX_JAHRE,		99

.equ LF, 0x0A
.equ SPACE, 0x20
.equ DOT, '.'
.equ COLON, ':'
.equ ASCII_Z, 0x30					// Zur Umwandlung Ziffer->ASCII-Zeichen 

.equ DCF77_0,	2500000	- 1			// Zeit fuer 0-Bit (100 ms), Taktfrequenz / 1000 * 100 - 1
.equ DCF77_1,	5000000	- 1			// Zeit fuer 1-Bit (200 ms), Taktfrequenz / 1000 * 200 - 1

.equ DCF77_INIT_L, 0x00120000		// DCF77-Initialwert: Bits 0-31
.equ DCF77_INIT_H, 0x0				// DCF77-Initialwert: Bits 32-58

/*
* Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
* die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
* dass die Register mit ihrem Reset-Wert vorbelegt sind!
*/
main:
// Initialisiere UART
	bl uart_init
	
	mov	r10, #0
	mov	r11, #1
	
	ldr	r0, =SIM_SCGC5_PORTA		
	str	r11, [r0]					// Port A aktivieren
	
	mov	r1, #PCR_GPIO				// Portbits (DCF77, LED) als GPIO konfigurieren 
	ldr	r0, =PORTA_PCR6
	str	r1, [r0]
	ldr	r0, =PORTA_PCR10			
	str	r1, [r0]

	ldr	r0, =DCF77_PDDR				// Portbits (DCF77, LED) als Ausgang konfigurieren	
	str	r11, [r0]	
	ldr r0, =LED_PDDR
	str r11, [r0]

	ldr	r0, =SIM_SCGC6_PIT				
	str	r11, [r0]					// PIT-Modul aktivieren
		
	ldr	r0, =PIT_MCR
	str	r10, [r0]					// Alle PITs aktivieren (MDIS: 0), Kein Stopp waehrend Debug (FRZ: 0)
	
	ldr	r0, =PIT_TCTRL0 
	str	r10, [r0]					// TEN: 0, TIE: 0, CHN: 0 (PIT0 deaktiviert, kein IRQ, keine Verkettung)

	ldr r0, =SCR_VTOR				// Basisadresse der IVT
	ldr r0, [r0]					// laden
	ldr r1, =isr_rtc_sec
	str r1, [r0, #IVT_RTC_SEC_OFS]	// RTC-ISR eintragen
	ldr r1, =isr_pit0
	str r1, [r0, #IVT_PIT0_OFS]		// PIT0-ISR eintragen

	ldr	r0, =SIM_SCGC6_RTC			
	str	r11, [r0]					// RTC-Modul aktivieren

	ldr	r0, =RTC_CR_SWR				// RTC-Software-Reset aktivieren/deaktiviren
	str	r11, [r0]
	str	r10, [r0]

/* R/W Access Control Register (RTC_WAR/_RAR)
 * Diese Register werden vom Software Reset nicht geaendert!
 * Sollte eines der Bits in diesen Registern 0 sein, laesst es sich danach nicht mehr auf 1 setzen!
 * Es wird erst nach dem naechsten POR (Power On Reset) automatisch wieder gesetzt.
 * Das Setzen von Bits in diesen beiden Registern ist daher zwecklos.
 */
 	
	ldr	r4, =RTC_BASE
	mov	r1, #BIT_TSIE				// Nur Sekunden-Interrupt aktivieren		
	str	r1, [r4, #O_RTC_IER]		// (nach Reset sind alle bis auf TSIE aktiviert!)
	
	ldr r0, =RTC_CR_OSCE
	str r11, [r0]					// 32-kHz-Oszillator aktivieren...
	
	ldr r0, =str_start
	bl uart_putString
	
	ldr r0, =OSC_DELAY				// ... und einschwingen lassen
rtc_osc_wait:
	subs r0, r0, #1
	bne rtc_osc_wait

	ldr r0, =str_end
	bl uart_putString
	
	mov r1, #(NVIC_RTC_SEC | NVIC_PIT0)	//  RTC-Sekunden- und PIT0-Interrupt im NVIC zulassen
	ldr r0, =NVIC_ICPR2
	str r1, [r0] 
	ldr r0, =NVIC_ISER2
	str r1, [r0]

	ldr r0, =str_init				// Text zur Bedienung					
	bl uart_putString

	bl get_date						// Datum und Zeit einlesen
	mov r6, r0						// Datum eintragen/sichern
	mov r7, r1
    str r10, [r4, #O_RTC_TPR]		// Vor Beschreiben des TSR den Prescaler auf Null setzen
    str r1, [r4, #O_RTC_TSR]		// Sekunden eintragen
	ldr	r5, =RTC_SR_TCE 			
    str r11, [r5]					// RTC aktivieren
	bl print_date					// Datum und Zeit ausgeben

/* Register vorbelegen	
	R4: RTC_BASE
	R5: RTC_SR_TCE
	R6: Datum
	R7: aktuelle Sekunde (Index)
	R8: DCF77-Bits 0..31
	R9: DCF77-Bits 32..58
	R10: #0
	R11: #1
*/

main_loop:
	bl uart_charPresent				// Zeicheneingabe?
	cbz r0, no_new_date				// nein
	bl uart_getChar
	cmp r0, #'n'					
	bne no_new_date
	
	bl get_date						// Datum und Zeit einlesen
	mov r6, r0						// Datum eintragen/sichern
    str r10, [r5]					// RTC deaktivieren
    str r10, [r4, #O_RTC_TPR]		// Vor Beschreiben des TSR den Prescaler auf Null setzen
    str r1, [r4, #O_RTC_TSR]		// Zeit (in Sekunden) eintragen
    str r11, [r5]					// RTC aktivieren
    
no_new_date:	
	ldr r1, =sec_flag
	ldr r0, [r1]
	cmp r0, #1						// Sekunde abgelaufen?
	bne main_loop
	
	str r10, [r1]					// Sekunden-Flag zuruecksetzen

erster_durchlauf:
    ldr r3, [r4, #O_RTC_TSR]		// aktuelle Zeit holen

    ldr r0, =TAG_SEKUNDEN
    cmp r3, r0						// Tagesueberlauf pruefen
    blt kein_ueberlauf

    mov r3, #0						// Ueberlauf -> Zeit auf Null setzen
    
    str r10, [r5]					// RTC deaktivieren
    str r10, [r4, #O_RTC_TPR]		// Prescaler & Sekunden auf Null setzen
    str r10, [r4, #O_RTC_TSR]
    str r11, [r5]					// RTC aktivieren

    ubfx r0, r6, #0, #8				// Wochentag auslesen

    add r0, r0, #1					// Wochentag erhoehen & auf Ueberlauf prüfen
    cmp r0, #WOCHENTAGE
    it gt
    movgt r0, #1					// Bei Ueberlauf auf Montag setzen
    bfi r6, r0, #0, #8				// Wochentag zurueckschreiben

    ubfx r0, r6, #8, #8				// Tag auslesen
    ubfx r1, r6, #16, #8			// Monat auslesen

    add r0, r0, #1					// Tag erhoehen
    bfi r6, r0, #8, #8				// und zurueckschreiben

    ldr r2, =table_tpm				// max. Tage des aktuellen Monats laden
    ldrb r2, [r2, r1]

    cmp r0, r2 						// Letzter Tag des Monats ueberschritten?
    ble kein_ueberlauf				// Sprung, wenn nicht

    bfi r6, r11, #8, #8				// Tag auf Eins setzen			

    add r1, r1, #1					// Monat erhoehen
    bfi r6, r1, #16, #8				// und zurueckschreiben

    cmp r1, #MAX_MONATE 			// Dezember ueberschritten?
    ble kein_ueberlauf				// Sprung, wenn nicht

    bfi r6, r11, #16, #8			// Monat auf Januar setzen

    ubfx r0, r6, #24, #8			// Jahr auslesen
    add r0, r0, #1					// erhoehen
    cmp r0, #MAX_JAHRE				// Jahrhundertueberlauf prufen
    it gt
    movgt r0, #0					// Bei Ueberlauf auf Null setzen
    bfi r6, r0, #24, #8				// und zurückschreiben

kein_ueberlauf:
    mov r2, #MINUTEN_SEKUNDEN
    udiv r0, r3, r2					// Minuten berechnen
    mls r7, r0, r2, r3				// aktuelle Sekunde ermitteln

    cmp r7, #0						// Terminalausgabe nur in Sekunde 0
    bne nicht_sekunde_null

    mov r1, r6						// Parameter: R0: Zeit (Minuten), R1: Datum
    bl dcf77_bits					// Bits fuer das DCF77-Signal zusammensetzen
        
    mov r8, r0						// DCF77-Bits (R0/R1) sichern
    mov r9, r1

    mov r0, r6						// Datum & Zeit als Parameter
    ldr r1, [r4, #O_RTC_TSR]		// aktuelle Zeit holen
	bl print_date					// Datum und Zeit ausgeben

    mov r0, r8						// DCF77-Bits uebergeben
    mov r1, r9
    bl bitstream					// Bitstrom ausgeben

nicht_sekunde_null:
    cmp r7, #59						// Kein Impuls in Sekunde 59
    beq main_loop

    and r1, r7, #0x1F				// Bit-Nr. auf 0..31 begrenzen  
    lsl r1, r11, r1					// Eine Eins n-mal (n=aktuelle Sekunde) verschieben -> aktuelles DCF77-Bit

    cmp r7, #32						// Anhand der Sekunden (R7) entscheiden,
    ite lt
    andlt r0, r8, r1				// ob das Bit aus R8 (Bits 0..31) 
    andge r0, r9, r1				// oder R9 (Bits 32..58) kommt

    cmp r0, #0						// Kurzen (0) oder langen (1) DCF77-Impuls waehlen
    ite eq
    ldreq r0, =DCF77_0				// und entsprechenden Wert laden
    ldrne r0, =DCF77_1

    ldr r3, =PIT_LDVAL0				// PIT0 vorbereiten
    str r0, [r3]

    ldr r1, =DCF77_PDOR				// DCF77-Signal -> L-Pegel
    str r10, [r1]
    ldr r1, =LED_PDOR				// LED an
    str r10, [r1]

    mov r0, #PIT_START
    ldr r1, =PIT_TCTRL0
    str r0, [r1]					// PIT0 starten

	b main_loop

/*** Unterprogramme und ISR ***/

/*
 * Gibt den DCF77-Bitstrom auf dem Terminal aus
 * Parameter: DCF77-Bits, R0: untere 32 Bit, R1: obere 27 Bit
 * Rueckgabe: keine
*/
.thumb_func
bitstream:
    push {r4-r6, lr}
    mov r4, r0						// DCF77-Bits sichern
    mov r5, r1

    ldr r0, =str_dcf77				// Terminalausgabe
    bl uart_putString

    mov r6, #59						// Bitzaehler initialisieren
bs_loop:    
    rrxs r5, r5						// LSB[R5] -> Carry
    rrxs r4, r4						// Carry -> MSB[R4]; LSB[R4] -> Carry
    ite cs							// anhand des Carry-Flags entscheiden, ob eine 0 oder eine 1 ausgegeben wird 
    movcs r0, #'1'
    movcc r0, #'0'
    bl uart_putChar
    subs r6, r6, #1					// Zaehler dekrementieren
    bne bs_loop						// alle Bits durchlaufen?
    
	mov r0, #LF						// Neue Zeile
	bl uart_putChar	
	pop {r4-r6, pc}
    
/*
 * Setzt aus Datum und Zeit die Bits fuer das DCF77-Signal zusammen
 * Parameter: R0: Zeit (in Minuten), R1: Datum (0xYYMMDDWT (YY: Jahr, MM: Monat, DD: Tag, WT: Wochentag))
 * Rueckgabe: R0: DCF77-Bits (0..31), R1: DCF77-Bits (32..58)  
*/
.thumb_func
dcf77_bits:
    push {r4-r7}

    mov r2, #STUNDEN_MINUTEN
    udiv r4, r0, r2					// Stunden
    mls r5, r4, r2, r0				// Minuten
    
    mov r6, r1						// Datum sichern
    mov r7, #10						// Divisor/Multiplikator
    
    mov r0, #DCF77_INIT_L			// DCF77-Bits (L) initialisieren
    mov r1, #DCF77_INIT_H			// DCF77-Bits (H) initialisieren

    udiv r2, r4, r7					// Stunden-Zehner
    bfi r1, r2, #1, #2				// in Bitstrom eintragen
    mls r3, r7, r2, r4				// Stunden-Einer
    bfi r0, r3, #29, #3				// aufgeteilt
    orr r1, r1, r3, lsr #3			// in Bitstrom eintragen

    eor r3, r2, r3					// Stunden-Paritaet berechnen
    eor r3, r3, r3, lsr #2
    eor r3, r3, r3, lsr #1
    bfi r1, r3, #3, #1				// und in Bitstrom eintragen

    udiv r2, r5, r7					// Minuten-Zehner
    bfi r0, r2, #25, #3				
    mls r3, r7, r2, r5				// Minuten-Einer
    bfi r0, r3, #21, #4

    eor r3, r2, r3					// Minuten-Paritaet berechnen
    eor r3, r3, r3, lsr #2
    eor r3, r3, r3, lsr #1
    bfi r0, r3, #28, #1				// und eintragen

    ubfx r2, r6, #0, #8				// Wochentag auslesen
    bfi r1, r2, #10, #3

    ubfx r2, r6, #8, #8				// Tag auslesen
    udiv r3, r2, r7					// Tag-Zehner
    bfi r1, r3, #8, #2
    mls r3, r7, r3, r2				// Tag-Einer
    bfi r1, r3, #4, #4

    ubfx r2, r6, #16, #8			// Monat auslesen
    udiv r3, r2, r7					// Monat-Zehner
    bfi r1, r3, #17, #1
    mls r3, r7, r3, r2				// Monat-Einer
    bfi r1, r3, #13, #4

    ubfx r2, r6, #24, #8			// Jahr auslesen
    udiv r3, r2, r7					// Jahr-Zehner
    bfi r1, r3, #22, #4
    mls r3, r7, r3, r2				// Jahr-Einer
    bfi r1, r3, #18, #4

    ubfx r3, r1, #4, #22			// Datum komplett auslesen
    eor r3, r3, r3, lsr #16			// Datums-Paritaet berechnen
    eor r3, r3, r3, lsr #8
    eor r3, r3, r3, lsr #4
    eor r3, r3, r3, lsr #2
    eor r3, r3, r3, lsr #1
    bfi r1, r3, #26, #1				// und eintragen

    pop {r4-r7}
    bx lr
    
/*
 * Fragt Wochentag, Datum und Zeit vom Terminal ab 
 * Parameter: keine
 * Rueckgabe: R0: 0xYYMMDDWT (YY: Jahr, MM: Monat, DD: Tag, WT: Wochentag), R1: Tageszeit (Sekunden)
 */	
.thumb_func
get_date:
	push {r4, r5, lr}
	
	ldr	r0, =str_wtag				// Wochentag abfragen
	bl	uart_putString
	bl	uart_getChar				// ein einzelnes Zeichen genuegt
	and	r0, r0, #0x07				// WT: [1..7]
	bfi r4, r0, #0, #8
	bl	uart_putByteBase10

	ldr	r0, =str_tag				// Tag abfragen
	bl	uart_putString
	bl	uart_getByteBase10
	bfi r4, r0, #8, #8
	bl	print_decimal

	ldr	r0, =str_monat				// Monat abfragen
	bl	uart_putString
	bl	uart_getByteBase10
	bfi r4, r0, #16, #8
	bl	print_decimal

	ldr	r0, =str_jahr				// Jahr abfragen
	bl	uart_putString
	bl	uart_getByteBase10
	bfi r4, r0, #24, #8
	bl	print_decimal

	ldr	r0, =str_stunde				// Stunden abfragen
	bl	uart_putString			
	bl	uart_getByteBase10
	mov r5, r0
	bl	print_decimal

	ldr	r0, =str_minute				// Minuten abfragen
	bl	uart_putString
	bl	uart_getByteBase10
	mov r1, #STUNDEN_MINUTEN
	mla r5, r5, r1, r0				// Stunden in Minuten umrechnen und aufaddieren
	bl	print_decimal
	
	mov r0, #LF
	bl uart_putChar
	
	mov r0, r4						// Datum zurueckgeben
	mov r2, #MINUTEN_SEKUNDEN		// Rueckgabe in R0 und R1 (da Ergebnis nicht in 32 bit passt)
	mul r1, r5, r2					// Summe der Minuten in Sekunden umrechnen

	pop {r4, r5, pc}
	
/*
 * Gibt Datum und Zeit in ueblicher Form aus.
 * Parameter: R0: YYMMDDWT, R1: Tageszeit in Sekunden
 * Rueckgabe: keine
 */	
.thumb_func
print_date:
	push {r4, r5, lr}
	
	mov r4, r0						// Parameter sichern
	mov r5, r1
	
	mov r0, #LF						// Neue Zeile
	bl uart_putChar
	
	and r1, r4, #0x07				// Wochentag: [1..7]
	ldr r2, =table_wtag
	ldr r0, [r2, r1, lsl #2]
	bl uart_putString
	
	ubfx r0, r4, #8, #8				// Tag auslesen (Position innerhalb R4, Laenge = 1 Byte)
	bl print_decimal

	mov r0, #DOT
	bl uart_putChar
	
	ubfx r0, r4, #16, #8			// Monat auslesen
	bl print_decimal

	mov r0, #DOT
	bl uart_putChar
	
	ubfx r0, r4, #24, #8			// Jahr auslesen
	bl print_decimal

	mov r0, #SPACE
	bl uart_putChar
	
	mov r1, #STUNDEN_SEKUNDEN		// Stunden berechnen
	udiv r0, r5, r1
	mls r4, r0, r1, r5				// Rest-Sekunden berechnen  
	bl print_decimal
	
	mov r0, #COLON
	bl uart_putChar
	
	mov r1, #MINUTEN_SEKUNDEN
	udiv r0, r4, r1					// Minuten berechnen 
	bl print_decimal
	
	ldr r0, =str_uhr
	bl uart_putString

	pop {r4, r5, pc}
	
/*
 * Gibt eine Dezimalzahl mit führender Null aus.
 * Parameter: R0: Wert
 * Rueckgabe: keine
 */	
.thumb_func
print_decimal:
	push {lr}	
	cmp	r0, #10
	ite	lt
	ldrlt r1, =uart_putByte
	ldrge r1, =uart_putByteBase10
	blx	r1
	pop	{pc}

/*
 * ISR fuer den Sekunden-IRQ
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
isr_rtc_sec:
	ldr r1, =sec_flag			// Sekunden-Flag setzen
	mov r0, #1
	str r0, [r1]
	bx lr 

/*
 * ISR fuer PIT0
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
isr_pit0:
    ldr r0, =PIT_TCTRL0			// PIT anhalten
    mov r1, #0
    str r1, [r0]

    ldr r0, =PIT_TFLG0			// IRQ-Flag loeschen
    mov r1, #1
    str r1, [r0]

    ldr r0, =DCF77_PDOR			// DCF77-Signal -> H-Pegel
    str r1, [r0]
    ldr r0, =LED_PDOR			// LED -> aus
    str r1, [r0]

    bx lr


/*** Datenbereich (ab 0x20000000) ***/
.data
sec_flag: 	.word 0

// Wochentage (Montag bei Index 1, 0 = Fehler) 
table_wtag:	.word str_WTF, str_Mo, str_Di, str_Mi, str_Do, str_Fr, str_Sa, str_So

// Tage pro Monat, Array-Index beinnt bei Null, aber Monat=0 ist nicht moeglich
table_tpm:	.byte 0,31,28,31,30,31,30,31,31,30,31,30,31

str_WTF:	.asciz "Fehler!, "
str_Mo:		.asciz "Montag, "
str_Di:		.asciz "Dienstag, "
str_Mi:		.asciz "Mittwoch, "
str_Do:		.asciz "Donnerstag, "
str_Fr:		.asciz "Freitag, "
str_Sa:		.asciz "Samstag, "
str_So:		.asciz "Sonntag, "

str_start:	.asciz "\nOszillator einschwingen lassen (ca. 1 s)..."
str_end:	.asciz "fertig.\n\n"
 
str_init:	.asciz "Ein neues Datum kann mit [n] angefordert werden.\n"
str_stunde:	.asciz "\nStunde eingeben (hh): "
str_minute:	.asciz "\nMinute eingeben (mm): "
str_tag:	.asciz "\nTag eingeben (TT): "
str_wtag:	.asciz "\nWochentag eingeben (Montag = 1, ..., Sonntag = 7): "
str_monat:	.asciz "\nMonat eingeben (MM): "
str_jahr:	.asciz "\nJahr eingeben (JJ): "

str_dcf77:	.asciz "DCF77-Bitstrom: "
str_uhr:	.asciz " Uhr\n"


/************************************************************
Versuch: 6-3
Name: 
Matrikel-Nr.: 
Zeitbedarf: 
************************************************************/

// Include-Dateien
.include "k1514.inc"				// Praktikumsspezifische Definitionen
.include "lib_uart.inc"				// Unterprogramme zur Ein-/Ausgabe vom/zum Terminal (CuteCom)

// Assemblerdirektiven
.text								// hier beginnt ein Code-Segment
.align	2							// Ausrichtung an eine gerade Adresse
.global	main						// "main" wird als globales Symbol deklariert
.syntax unified

.equ VTOR_BASE,		0x1FFFFE00		// Adressregister für die Vektortabelle.

.equ SIM_SCGC5,		0x40048038
.equ SIM_SCGC6,		0x4004803C

// NVIC
.equ NVIC_ISER1,	0xE000E104	
.equ NVIC_ICPR1,	0xE000E284

// Port A
.equ PORTA_BASE, 	0x40049000
.equ O_PCR6,		0x18			// Signal FTM0_CH3 auf Pin PTA6
.equ O_PCR10,		0x28			// LED (bl)	PWM
.equ O_PCR11,		0x2C			// LED (or) Polaritaet

.equ GPIOA_PDOR,	0x400FF000
.equ GPIOA_PDDR,	0x400FF014

// FTM-Register
.equ FTM0_SC,		0x40038000

// Fuer Offset-Adressierung
.equ FTM0_BASE, 	FTM0_SC
.equ O_FTM_C0SC,	0x0C
.equ O_FTM_C3SC,	0x24
.equ O_FTM_C3V,		0x28
.equ O_FTM_MODE,	0x54
.equ O_FTM_MOD,		0x08
.equ O_FTM_CNT,		0x04

// Offsets und Initalwerte fuer die erweiterten FTM-Register
.equ O_FTM_CNTIN,	0x4C
.equ O_FTM_SYNC,	0x58
.equ O_FTM_OUTINIT,	0x5C
.equ O_FTM_OUTMASK,	0x60
.equ O_FTM_COMBINE,	0x64
.equ O_FTM_EXTTRIG,	0x6C
.equ O_FTM_POL,		0x70
.equ O_FTM_FILTER,	0x78
.equ O_FTM_FLTCTRL,	0x7C
.equ O_FTM_QDCTRL,	0x80
.equ O_FTM_CONF,	0x84
.equ O_FTM_SYNCCONF,0x8C
.equ O_FTM_INVCTRL,	0x90
.equ O_FTM_PWMLOAD,	0x98

.equ CONF_VAL,		0xc0			// BDMMODE = 11 (Mode: Funcional)
.equ COMBINE_VAL,	1 << 13			// PWM-synchronisation (CH2 & CH3) aktivieren

// Bit-banding
.equ FTM0_MODE,		FTM0_BASE + O_FTM_MODE
.equ FTM0_C3SC,		FTM0_BASE + O_FTM_C3SC
.equ FTM0_PWMLOAD,	FTM0_BASE + O_FTM_PWMLOAD

__BBREG SCGC5_PORTA, SIM_SCGC5, 9	// PortA (FTM)
__BBREG PDDR_A10, GPIOA_PDDR, 10
__BBREG PDOR_A10, GPIOA_PDOR, 10
__BBREG PDDR_A11, GPIOA_PDDR, 11
__BBREG PDOR_A11, GPIOA_PDOR, 11

__BBREG SCGC6_FTM0, SIM_SCGC6, 24	// FTM

__BBREG FTM0_C3SC_ELSA, FTM0_C3SC, 2		// H-Pulse: ELSnB=1, ELSnA=0 / L-Pulse: ELSnB=x, ELSnA=1
											// Mit ELSnB=1, ELSnA = 0 => H-Pulse, ELSnA=1 => L-Pulse 
__BBREG FTM0_MODE_WPDIS, FTM0_MODE, 2		// WPDIS-Bit

__BBREG FTM0_PWMLOAD_LDOK, FTM0_PWMLOAD, 9	// Synchronisation aktivieren

// Konstanten									
.equ IRQ_FTM0_OFFSET, 0x138			// Offset für ISR
.equ IRQ_FTM0_BITPOS, 1<<30			// Bitposition für NVIC

.equ PCR_ALT1, 0x120				// GPIO, Open-Drain fuer LED
.equ PCR_ALT3, 0x300				// ALT3 (FTM0_CH3)

.equ SC_TOF_MASK, 0x80				// Maske fuer das TOF-Bit

.equ SC_ENABLE, 0x48				// TOIE = 1 (IRQ aktiviert), CPWMS=0 (aufwärts zählen), CLKS = 01 (Systemtakt), PS = 000 (kein Vorteiler)
.equ SC_DISABLE, 0x0

// PWM Zielfrequenz = 1 kHz => 1 ms Periodendauer, Systemtakt = 25 MHz
//	- CNTIN = 0
//	- FTM Zählerfrequenz: kein Vorteiler => 25 MHz
//	- PWM Periode = (MOD - CNTIN + 0x0001) * FTM Zählerperiode (s. S. 1000)
//		<=> PWM Periode = (MOD + 0x0001) * (1/FTM Zählerfrequenz)
//		<=> PWM Periode * FTM Zählerfrequenz = MOD + 0x0001
//		=> MOD = 24999
.equ MOD_VAL, 24999

.equ BASE_PERCENT, 5				// 5% Offset 
.equ C3V_BASE, (MOD_VAL * BASE_PERCENT) / 100	// Default-Impulsdauer 

.equ WPDIS_MASK, 0x4				// FTMEN=0: TPM-compatible only, WPDIS=1: WP disabled, PWMSYNC=0: No restrictions, CAPTEST=0: Capture Test disabled
									// FAULTTM=0: Fault Control disabled, FAULTIE=0: Fault Control IRQ disabled

.equ C3SC_CHF_MASK, 0x80			// Maske für CHF-Bit

.equ C3SC_DEFAULT, 0x78				// IRQ aktiviert, Edge aligned PWM, H-Pegel, kein DMA

.equ LF, 0x0a
.equ PROMPT, '>'
.equ PLUS, '+'
.equ MINUS, '-'
.equ CHAR_0, '0'
.equ CHAR_9, '9'
.equ CHAR_L, 'L'
.equ CHAR_H, 'H'

/* 
 * Es gibt grundsaetzlich zwei Alternativen, um die Vorgaben der Versuchsbeschreibung zu erfuellen.
 *
 * Alternative 1: FTMEN=0
 * Damit werden alle erweiterten Funktionen des FTM deaktiviert und es steht nur der (aeltere) TPM-Modus zur Verfuegung.
 * Aber schon dieser ist fuer diesen Versuch ausreichend.
 *
 * Alternative 2: FTMEN=1
 * Es wird die volle Funktionalitaet des FTM aktiviert. Das bedeutet allerdings auch einen deutlichen Mehraufwand bei der 
 * Initialisierung und die Beachtung der Synchronisation der Pulsweite.
*/
// Auswahl des Modus (bedingte Assemblierung)
.equ FTMEN, 1		// 0: TPM, 1: FTM

/*
 * Hauptprogramm (main): Wird nicht direkt nach dem Reset, sondern von einer Routine aufgerufen, 
 * die nach einem Reset die grundlegende Systeminitialisierung durchfuehrt. Man darf also nicht davon ausgehen,
 * dass die Register mit ihrem Reset-Wert vorbelegt sind.
 */
main:	
	bl	uart_init

	mov	r10, #0
	mov	r11, #1		

	ldr	r0, =SCGC5_PORTA			// Takt für Port A aktivieren 
	str	r11, [r0]
	
	ldr	r0, =PORTA_BASE	
	mov	r1, #PCR_ALT3
	str	r1, [r0, #O_PCR6]			// PTA6 als FTM0_CH3
	mov	r1, #PCR_ALT1
	str	r1, [r0, #O_PCR10]			// PTA10 (blaue LED) als GPIO (ODE) fuer PWM
	str r1, [r0, #O_PCR11]			// PTA11 (orange LED) fuer Polaritaet  
	
	ldr r0, =PDDR_A10				// PTA10 & PTA11 als Ausgang
	ldr r1, =PDDR_A11
	str r11, [r0]
	str r11, [r1]

	ldr	r0, =SCGC6_FTM0				// Takt für FTM0 aktivieren
	str	r11, [r0]
	
	ldr	r0, =FTM0_MODE_WPDIS		// Schreibschutz deaktivieren
	str r11, [r0]					
	
	ldr r4, =FTM0_BASE
	mov	r1, #WPDIS_MASK	| FTMEN		// MODE initialisieren, WPDIS aktiviert halten
	str	r1, [r4, #O_FTM_MODE]
	
	mov	r0, #SC_DISABLE				// FTM0 anhalten
	str	r0, [r4]					// R4: FTM0_SC = FTM_BASE

/* Fuer FTMEN=1 muessen weitere Register initialisiert werden!
 * Deren Resetwerte darf man zwar - wie immer - zur Orientierung heranziehen, aber nicht voraussetzen!
 * Alle Register des erweiterten Satzes, die hier nicht initialisiert werden, sind fur diesen Versuch 
 * nicht relevant oder deaktiviert.
 */	
.if FTMEN == 1						// Nur einbeziehen, wenn FTMEN=1
	mov r1, #CONF_VAL
	str r1, [r4, #O_FTM_CONF]		// BDMMODE setzen

	str r10, [r4, #O_FTM_CNTIN]		// initialer Zaehlerwert (0)
	str r10, [r4, #O_FTM_POL]		// Safe-Mode Polaritaet High
	str r10, [r4, #O_FTM_OUTMASK]	// Alle Kanaele aktiv
	str r10, [r4, #O_FTM_EXTTRIG]	// Alle Trigger deaktiviert
	str r10, [r4, #O_FTM_FILTER]	// Keine Filter
	str r10, [r4, #O_FTM_FLTCTRL]	// Fehler-Eingaenge deaktiviert
	str r10, [r4, #O_FTM_QDCTRL]	// Quadratur-Decoder deaktiviert
	str r10, [r4, #O_FTM_INVCTRL]	// Keine Kanal-Invertierung
	str r10, [r4, #O_FTM_PWMLOAD]	// Aktualisierung deaktiviren (initial)
	str r10, [r4, #O_FTM_SYNC]		// Keine Synchronisation per Trigger
	str r10, [r4, #O_FTM_SYNCCONF]	// Legacy Synchronisation aktivieren
	
	mov r1, #COMBINE_VAL
	str r1, [r4, #O_FTM_COMBINE]	// PWM-Synchronisation fuer CH3 (& CH2) aktivieren 
.endif

	add r2, r4, #O_FTM_C0SC			// Interrupts aller Kanaele deaktivieren
	mov r3, #8						// C0SC..C7SC
cnsc_loop:
	str r10, [r2], #8				// Adress-Abstand = 8
	subs r3, r3, #1
	bne cnsc_loop
		
	ldr	r1, [r4, #O_FTM_C3SC]		// C3SC zum Zuruecksetzen von CHF lesen
	mov	r1, #C3SC_DEFAULT			// IRQ aktivieren, Modus setzen
	str	r1, [r4, #O_FTM_C3SC]
	
	str	r10, [r4, #O_FTM_CNT]		// CNT beschreiben, um Zähler aus CNTIN (Default=0, wenn FTMEN=0) zu laden
	
	ldr	r1, =MOD_VAL
	str	r1, [r4, #O_FTM_MOD]		// Periodendauer setzen
	
	ldr	r1, =C3V_BASE		
	str	r1, [r4, #O_FTM_C3V]		// Impulsdauer setzen
 
	ldr	r0, =VTOR_BASE				// ISR in IVT eintragen
	ldr	r1, =isr_ftm0
	str	r1, [r0, #IRQ_FTM0_OFFSET]
	
	mov	r1, #IRQ_FTM0_BITPOS	
	ldr	r0, =NVIC_ICPR1				// ggf. hängende Interrupts löschen
	str	r1, [r0]	
	ldr	r0, =NVIC_ISER1				// Interrupt in NVIC aktivieren
	str	r1, [r0]

	mov	r1, #SC_ENABLE				// FTM0 starten
	str	r1, [r4]					// R4: FTM0_SC

/*** Register vorbelegen ***/
/*
	R4: FTM0_SC
	R10: 0
	R11: 1
*/
	mov	r5, #10						// Multiplikator fuer Impulsdauerberechnung
	ldr	r6, =l_puls
	ldr	r7, =impulsdauer
	ldr	r8, =MOD_VAL
	mov r9, #BASE_PERCENT
	
	ldr	r0, =str_impulsdauer		// Impulsdauer (%) ausgeben
	bl uart_putString
	mov	r0, r9
	bl uart_putByteBase10
	
	ldr	r0, =str_prozent
	bl uart_putString
	
main_loop:
	mov	r0, #PROMPT					// Prompt ausgeben
	bl	uart_putChar
	
	bl	uart_getChar				// Terminaleingabe abfragen
	mov	r4, r0						// Eingabe sichern 
	
	bl	uart_putChar				// Eingabe-Echo
	
	mov	r0, #LF						// Neue Zeile
	bl	uart_putChar
	
	cmp	r4, #PLUS					// '+' ?
	itt	eq
	streq r10, [r6]					// H-Pulse einstellen
	moveq r0, #CHAR_H
	beq pr_puls
	
	cmp	r4, #MINUS					// '-' ?
	itt	eq
	streq r11, [r6]					// L-Pulse einstellen
	moveq r0, #CHAR_L
	bne	no_puls
	
pr_puls:
	bl uart_putChar					// Polaritaet ausgeben
	ldr r0, =str_puls
	bl uart_putString
	
no_puls:
	cmp	r4, #CHAR_0					// kleiner '0' ?
	blo	main_loop					// dann ungültige Eingabe
	cmp	r4, #CHAR_9					// größer '9' ?
	bhi	main_loop					// dann ungültige Eingabe
	
	mov R2, #100					// Divisor fuer Impulsdauerberechnung
	and r0, r4, #0x0F				// Impulsdauer berechnen
	mla r4, r0, r5, r9				
	mul	r1, r8, r4			
	udiv r1, r1, r2
	str r1, [r7]					// berechnete Impulsdauer speichern

	ldr	r0, =str_impulsdauer		// Impulsdauer (%) ausgeben
	bl uart_putString
	mov	r0, r4
	bl uart_putByteBase10
	
	ldr	r0, =str_prozent
	bl uart_putString
	
	b main_loop		

/*
 * ISR fuer FMT0
 * Parameter: keine
 * Rueckgabe: keine
 */		
.thumb_func
isr_ftm0:
	ldr r2, =PDOR_A10				// LED (blau)
	ldr r3, =FTM0_BASE				// FTM0_SC
	
	ldr	r0, [r3, #O_FTM_C3SC]								
	ands r1, r0, #C3SC_CHF_MASK		// CHF maskieren	
	beq	isr_ftm0_tof				// Wenn kein Channel-IRQ, TOF prüfen
	
	bic	r0, r0, #C3SC_CHF_MASK		// CHF löschen
	str	r0, [r3, #O_FTM_C3SC]					

	ldr	r0, =impulsdauer			
	ldr r1, [r0]					// Impulsdauer laden
	str	r1, [r3, #O_FTM_C3V]		// Impulsdauer eintragen

	mov r1, #1
	str r1, [r2]					// blaue LED aus
	
.if FTMEN == 1						// Nur einbeziehen, wenn FTMEN=1
	ldr r0, =FTM0_PWMLOAD_LDOK		// C3V aus Puffer uebernehmen
	str r1, [r0]
.endif	
	
	b isr_ftm0_return				// zum Rücksprung

isr_ftm0_tof:	
	ldr	r0, [r3]								
	ands r1, r0, #SC_TOF_MASK		// TOF maskieren
	beq	isr_ftm0_return				// Wenn kein TOF-IRQ, ISR verlassen

	bic	r0, r0, #SC_TOF_MASK		// TOF löschen
	str	r0, [r3]

	ldr	r0, =l_puls					// Polarität lesen
	ldr r1, [r0]
	ldr	r0, =FTM0_C3SC_ELSA			// Polarität setzen (Bit-banding)
	str	r1, [r0]	
	
	ldr r0, =PDOR_A11
	str r1, [r0]					// Polaritaet auf LED 
	
	mov r1, #0
	str r1, [r2]					// blaue LED an
	
isr_ftm0_return:	
	bx		lr


/*** Datenbereich (ab 0x20000000) ***/
.data
impulsdauer: .word C3V_BASE

l_puls: .word 0

str_impulsdauer: .asciz "Impulsdauer = "
str_prozent: .asciz " %\n"
str_puls: .asciz "-Impuls\n"
