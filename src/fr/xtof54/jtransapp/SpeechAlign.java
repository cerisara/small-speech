package fr.xtof54.jtransapp;

import java.util.List;
import java.util.ArrayList;

public class SpeechAlign {
	// text = suite de groupes de mots séparés par des espaces
	// chaque groupe de mots = mots concurrents séparés par des virgules
	public static void align(List data, String text) {
		System.out.println("detjtrapp start building graph "+text);

		ArrayList<String[][]> allrules = new ArrayList<String[][]>();
		String[] groupes = text.split(" ");
		ArrayList<Token> toks = new ArrayList<Token>();
		for (int i=0;i<groupes.length;i++) {
			toks.add(new Token("mot"+i));
			String[] words = groupes[i].split(",");
			List<Token> lwords = new ArrayList<>();
			for (String w : words) lwords.add(new Token(w));
			// rules.dim1 = une dim par mot
			// rules.dim2 = une dim par char de la rule
			String[][] rules = StateGraph.getRules(lwords);
			allrules.add(rules);
		}
		// now creates a new set of rules where each "word" is actually a set of possible concurrent words
		String[][] rules = new String[allrules.size()][];
		for (int i=0;i<rules.length;i++) {
			String[][] r = allrules.get(i);
			int nch=0;
			for (int j=0;j<r.length;j++) nch+=r[j].length;
			rules[i]=new String[nch+1+r.length];
			rules[i][0]="(";
			int ci=1;
			for (int j=0;j<r.length-1;j++) {
				System.arraycopy(r[j],0,rules[i],ci,r[j].length);
				ci+=r[j].length;
				rules[i][ci++]="|";
			}
			System.arraycopy(r[r.length-1],0,rules[i],ci,r[r.length-1].length);
			ci+=r[r.length-1].length;
			rules[i][ci]=")";
			assert ci==rules[i].length-1;
		}

		System.out.println("detjtrapp nrule "+rules.length+" "+rules[0].length);

		StateGraph graph = new StateGraph(rules,toks,true);
		graph.printGraph();

		graph.viterbi(data, 0, data.size()-2);
		System.out.println("detjtrapp viterbi forward done");
		int[] timeline = graph.backtrack();
		String s = "";
		for (int i: timeline) s+=i+" ";
		System.out.println("detjtrapp viterbi backward "+s);
	}
}
