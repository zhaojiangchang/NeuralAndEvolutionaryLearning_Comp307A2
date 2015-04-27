/*
 * This file is part of JGAP.
 *
 * JGAP offers a dual license model containing the LGPL as well as the MPL.
 *
 * For licensing information please see the file license.txt included with JGAP
 * or have a look at the top of class org.jgap.Chromosome which representatively
 * includes the JGAP license policy applicable for any file delivered with JGAP.
 */
package comp307a2part2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.jgap.*;
import org.jgap.event.*;
import org.jgap.gp.*;
import org.jgap.gp.function.*;
import org.jgap.gp.impl.*;
import org.jgap.gp.terminal.*;
import org.jgap.util.*;
import org.omg.CORBA.portable.InputStream;

/**
 * Simple example of Genetic Programming to discover the formula
 * (X > 0) OR (X == -8) OR (X == - 5)
 * given a set of inputs from -10 to 10 and expected outputs (true or false).
 *
 * The fitness function used in this example cares about deviation from the
 * expected result as well as the complexity of the solution (the easier the
 * solution, the better it is, mutatis mutandis).
 *
 * @author Klaus Meffert
 * @since 3.5
 */
public class Comp307a2part2 extends GPProblem {

	protected static Variable vx;
	private static Float[] x;
	private static Float[] y;
	public GPGenotype create() throws InvalidConfigurationException {
		GPConfiguration conf = getGPConfiguration();
		// The resulting GP program returns a boolean.
		// -------------------------------------------
		Class[] types = {CommandGene.BooleanClass};
		Class[][] argTypes = { {}
		};
		// The commands and terminals allowed to find a solution.
		// ------------------------------------------------------
		CommandGene[][] nodeSets = { {
			// We need a variable to feed in data (see fitness function).
			// ----------------------------------------------------------
			vx = Variable.create(conf, "X", CommandGene.FloatClass),
					new Multiply(conf, CommandGene.FloatClass),
					new Multiply3(conf, CommandGene.FloatClass),
					new Divide(conf, CommandGene.FloatClass),
					new Subtract(conf, CommandGene.FloatClass),
					new Pow(conf, CommandGene.FloatClass),
					new Terminal(conf, CommandGene.FloatClass, 2.0d, 10.0d, true),
		}
		};
		// Initialize the GPGenotype.
		// --------------------------
		return GPGenotype.randomInitialGenotype(conf, types, argTypes, nodeSets,
				100, true);
	}

	public void start() throws Exception {

		GPConfiguration config = new GPConfiguration();
		config.setMaxInitDepth(5);
		config.setPopulationSize(1000);
		config.setFitnessFunction(new FormulaFitnessFunction());
		config.setStrictProgramCreation(false);
		config.setProgramCreationMaxTries(5);
		config.setMaxCrossoverDepth(5);
		config.setCrossoverProb(75.0f);
		config.setMutationProb(25.0f);
		config.setReproductionProb(0.2f);
		// Lower fitness value is better as fitness value indicates error rate.
		// --------------------------------------------------------------------
		config.setGPFitnessEvaluator(new DeltaGPFitnessEvaluator());
		super.setGPConfiguration(config);
		GPGenotype geno = create();
		// Simple implementation of running evolution in a thread.
		// -------------------------------------------------------
		config.getEventManager().addEventListener(GeneticEvent.
				GPGENOTYPE_EVOLVED_EVENT, new GeneticEventListener() {
			public void geneticEventFired(GeneticEvent a_firedEvent) {
				GPGenotype genotype = (GPGenotype) a_firedEvent.getSource();
				int evno = genotype.getGPConfiguration().getGenerationNr();
				double freeMem = SystemKit.getFreeMemoryMB();
				if (evno % 100 == 0) {
					IGPProgram best = genotype.getAllTimeBest();
					System.out.println("Evolving generation " + evno);
					genotype.outputSolution(best);
				}
				if (evno > 3000) {
					System.exit(1);
				}
			}
		});
		config.getEventManager().addEventListener(GeneticEvent.
				GPGENOTYPE_NEW_BEST_SOLUTION, new GeneticEventListener() {
			/**
			 * New best solution found.
			 *
			 * @param a_firedEvent GeneticEvent
			 */
			public void geneticEventFired(GeneticEvent a_firedEvent) {
				GPGenotype genotype = (GPGenotype) a_firedEvent.getSource();
				IGPProgram best = genotype.getAllTimeBest();
				double bestFitness = genotype.getFittestProgram().
						getFitnessValue();
				if (bestFitness < 0.1) {
					// Quit, when the solutions seems perfect.
					// ---------------------------------------
					genotype.outputSolution(best);
					System.exit(0);
				}
			}
		});
		geno.evolve(10000);
	}

	public static void main(String[] args)
			throws Exception {
		Comp307a2part2 example = new Comp307a2part2();
		System.out.print("Enter file name: ");
		x = new Float[20];
		y = new Float[20];
		String file ="";
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			file = br.readLine();

		} catch (IOException ioe) {
			System.exit(1);
		}
		try {
			Scanner s = new Scanner(new File(file));
			s.nextLine();
			s.nextLine();
			int index = 0;
			while(s.hasNextFloat()){
				x[index] = s.nextFloat();
				y[index] = s.nextFloat();
				System.out.println(x[index]+"   "+y[index]);
				index++;
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Data training File caused IO exception");
		}
		example.start();
	}

	 class FormulaFitnessFunction extends GPFitnessFunction {
		protected double evaluate(final IGPProgram a_subject) {
			return computeRawFitness(a_subject);
		}

		public double computeRawFitness(final IGPProgram ind) {
			 System.out.println("aaaaa");

			double error = 0.0f;
			Object[] noargs = new Object[0];
			// Evaluate function for input numbers 0 to 20.
			// --------------------------------------------
			for (int i = 0; i < 20; i++) {
				// Provide the variable X with the input number.
				// See method create(), declaration of "nodeSets" for where X is
				// defined.
				// -------------------------------------------------------------
				vx.set(x[i]);
				try {
					// Execute the GP program representing the function to be
					// evolved.
					// As in method create(), the return type is declared as
					// float (see
					// declaration of array "types").
					// ----------------------------------------------------------------
					double result = ind.execute_float(0, noargs);
					// Sum up the error between actual and expected result to
					// get a defect
					// rate.
					// -------------------------------------------------------------------
					error +=Math.pow(Math.abs(result - y[i]),2);
					// If the error is too high, stop evlauation and return
					// worst error
					// possible.
					// ----------------------------------------------------------------
					if (Double.isInfinite(error)) {
						return Double.MAX_VALUE;
					}
				} catch (ArithmeticException ex) {
					// This should not happen, some illegal operation was
					// executed.
					// ------------------------------------------------------------
					System.out.println("x = " + x[i].floatValue());
					System.out.println(ind);
					throw ex;
				}
			}
			// In case the error is small enough, consider it perfect.
			// -------------------------------------------------------
			if (error < 0.001) {
				error = 0.0d;
			}
			return error;
		}
	}
}
