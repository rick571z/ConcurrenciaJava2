package robotsjcsp;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;

class PetNotificar {
	int robotId;
	int peso;

	public PetNotificar(int robotId, int peso) {
		this.robotId = robotId;
		this.peso = peso;
	}
}

// RoboFabCSP: Solución con replicación de canales
// Completad las líneas marcadas "TO DO"

public class RoboFabCSP implements RoboFab, CSProcess {

	// Un canal para notificarPeso
	Any2OneChannel chNotificar;
	// NUM_ROBOTS canales para permisoSoltar
	Any2OneChannel chSoltar[];
	// Un canal para solicitarAvance
	Any2OneChannel chAvanzar;
	// Un canal para contenedorNuevo
	Any2OneChannel chNuevo;

	public RoboFabCSP() {

		// Creamos los canales
		chNotificar = Channel.any2one();
		chSoltar = new Any2OneChannel[Robots.NUM_ROBOTS];
		for (int i = 0; i < Robots.NUM_ROBOTS; i++)
			chSoltar[i] = Channel.any2one();
		chAvanzar = Channel.any2one();
		chNuevo = Channel.any2one();
	}

	public void permisoSoltar(int robotId) {
		chSoltar[robotId].out().write(null);
	}

	public void notificarPeso(int robotId, int peso) {
		PetNotificar pet = new PetNotificar(robotId, peso);
		chNotificar.out().write(pet);
	}

	public void solicitarAvance() {
		chAvanzar.out().write(null);
	}

	public void contenedorNuevo() {
		chNuevo.out().write(null);
	}

	private boolean puedeAvanzar(int pendientes[], int peso) {
		boolean avanzar = true;
		for (int i = 0; i < pendientes.length && avanzar; i++) {
			if ((pendientes[i] + peso) <= Cinta.MAX_P_CONTENEDOR) {
				avanzar = false;
			}
		}
		return avanzar;
	}
	
	/**
	 * bucle del servidor
	 */
	public void run() {
		// Inicializamos y declaramos estado del recurso: peso, pendientes...
		int peso;
		// Array que contiene el peso que esta cargando cada robot en un momento
		// dado
		int pendientes[] = new int[Robots.NUM_ROBOTS];
		
		peso = 0;
		for (int i = 0; i < pendientes.length; i++) {
			pendientes[i] = 0;
		}

		// Estructuras para recepcion alternativa condicional:
		// reservamos NUM_ROBOTS entradas para permisoSoltar y una entrada cada
		// una de
		// notificarPeso, solicitarAvance y contenedorNuevo
		final AltingChannelInput[] guards = new AltingChannelInput[Robots.NUM_ROBOTS + 3];

		// Variables para determinar la posicion de Los canales
		// NOTIFICAR,AVANZAR y NUEVO en el array de canales
		final int NOTIFICAR = Robots.NUM_ROBOTS;
		final int AVANZAR = Robots.NUM_ROBOTS + 1;
		final int NUEVO = Robots.NUM_ROBOTS + 2;

		// reservamos NUM_ROBOTS entradas para permisoSoltar
		for (int i = 0; i < Robots.NUM_ROBOTS; i++) {
			guards[i] = chSoltar[i].in();
		}
		// Inicializacion de los canales mencionados en sus respectivas
		// posiciones
		guards[NOTIFICAR] = chNotificar.in();
		guards[AVANZAR] = chAvanzar.in();
		guards[NUEVO] = chNuevo.in();

		// array de booleanos para sincronizacion por condicion
		boolean enabled[] = new boolean[Robots.NUM_ROBOTS + 3];

		// inicializamos las condiciones de activacion de los canales
		// La cpre de notificarPeso y contenedorNuevo es true
		// No cambia
		enabled[NOTIFICAR] = true;
		enabled[NUEVO] = true;

		final Alternative services = new Alternative(guards);

		while (true) {
			// refrescamos el vector enabled:
			// evalucaion de la cpre solicitarAvance
			enabled[AVANZAR] = puedeAvanzar(pendientes, peso);
			
			/**
			 * Evaluacion de la CPRE permisoSoltar
			 */
			for (int i = 0; i < Robots.NUM_ROBOTS; i++) {
				enabled[i] = (peso + pendientes[i]) <= Cinta.MAX_P_CONTENEDOR;
			}

			// la SELECT:
			int i = services.fairSelect(enabled);

			if (i == NOTIFICAR) {
				PetNotificar leer = (PetNotificar) chNotificar.in().read();
				int p = leer.peso;
				int j = leer.robotId;
				pendientes[j] = p;
			
			} else if (i == AVANZAR) {
				chAvanzar.in().read();
			} else if (i == NUEVO) {
				// TO DO
				// TO DO
				chNuevo.in().read();
				peso = 0;
			} else { // permisoSoltar
				chSoltar[i].in().read();
				peso = peso + pendientes[i];
				pendientes[i] = 0;
			}
		}
	}
}
