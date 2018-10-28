package fr.xtof54.jtransapp;

import java.util.List;

public class SpeechAlign {
	// text = space separated words
	// TODO: il faut une grammaire et pas un texte ici
	public static void align(List data, String text) {
		StateGraph graph = StateGraph.quick(text);
		System.out.println("detjtrapp speechalign graph "+graph);

		// do Viterbi	
		// graph.viterbi(data, startFrame, endFrame);
		// int[] timeline = graph.backtrack();
	}
}
