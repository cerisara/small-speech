/*
This source code is copyrighted by Christophe Cerisara

It is licensed under the terms of the GPL-3
*/

package fr.xtof54.jtransapp.phonetiseur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import fr.xtof54.jtransapp.JTransapp;

/**
 * Classe permettant de stocker un dictionnaire de prononciation, type BDLex ou Morphalou
 * 
 * @author cerisara
 *
 */
public abstract class PronunciationsLexicon implements Serializable {
	/**
	 * Les phonemes utilises en sortie sont les suivants:
	 */
	final public static String[] phones = {"a","an","b","bb","d","e","E","eh","eu","euf","f","g","H","hh","i","in","j","J","k","l","m","n","o","O","oh","on","p","R","s","S","sil","swa","t","u","v","w","xx","y","z","Z"};

	/**
	 * associe un mot a ses prononciations possibles
	 */
	HashMap<String,Entree> dico=null;
	String curmot;

	
	/**
	 * ouvre un fichier texte et retourne un Iterator
	 * permettant de parcourir toutes les entrees (=lignes) textuelles contenues dans ce fichier
	 */
	public static Enumeration<String> getEntries(String fn, boolean isUTF) {
			// on a trouve un fichier qui existe: on utilise celui-ci
			final InputStream bif;
			try {
				if (fn.endsWith(".gz")) bif = new GZIPInputStream(JTransapp.main.getResources().getAssets().open(fn));
				else bif = JTransapp.main.getResources().getAssets().open(fn);
				
				// on a recupere un inputStream vers le fichier
				class MyEnum implements Enumeration<String> {
					BufferedReader bf = new BufferedReader(new InputStreamReader(bif));
					String nextLine;
					public MyEnum(boolean isUTF) {
						try {
							if (isUTF)
								bf = new BufferedReader(new InputStreamReader(bif,"UTF-8"));
							else
								bf = new BufferedReader(new InputStreamReader(bif,"ISO-8859-1"));
							nextLine = bf.readLine();
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						} catch (IOException e) {
							nextLine=null;
						}
					}
					public String nextElement() {
						String s=nextLine;
						try {
							nextLine = bf.readLine();
						} catch (IOException e) {
							nextLine=null;
						}
						return s;
					}
					public boolean hasMoreElements() {
						return (nextLine!=null);
					}
				}
				return new MyEnum(isUTF);
			} catch (IOException e) {
				System.out.println("detjtrapp "+fn+" "+e);
				e.printStackTrace();
				throw new RuntimeException(e);
			}
	}

	Entree sanstirets(String mot) {
		int i=mot.indexOf('-');
		if (i<0) return null;
		String[] ss = mot.split("-");
		// on cherche chaque partie individuellement puis on merge si on a TOUTES les parties
		Entree eall = new Entree();
		Entree e=null;
		for (i=0;i<ss.length;i++) {
			e = dico.get(ss[i]);
			if (e==null) return null;
			eall.phonesBase+=e.phonesBase;
		}
		if (e==null) return null;
		// je ne conserve QUE les liaisons possibles du dernier "mot" du mot compose
		eall.phonesOption=e.phonesOption;
		return eall;
	}
	
	public abstract String getRule(Entree e);
	
	public String getRule(String mot) {
		if (mot==null) return null;
		StringTokenizer st = new StringTokenizer(mot);
		if (st.countTokens()>1) {
			String s = "";
			while (st.hasMoreTokens()) {
				curmot = st.nextToken();
				s+=getRule(curmot);
			}
			return s;
		}
		curmot=mot;
		Entree e = dico.get(mot);
		if (e==null) {
			// mot inconnu !
			e = sanstirets(mot);
			if (e==null)
				return "";
		}
		if (e.autrePossible==null)
			return getRule(e);
		else {
			String s=" ( ";
			for (;;) {
				s+=getRule(e);
				e=e.autrePossible;
				if (e==null) break;
				else s+=" | ";
			}
			s+=" ) ";
			return s;
		}
	}
	
	public static String convertAccents(String s) {
		String r = s.replace("e1","é");
		r=r.replace("e2","è");
		r=r.replace("e3","ê");
		r=r.replace("e4","ë");
		r=r.replace("a2","à");
		r=r.replace("a3","â");
		r=r.replace("a4","ä");
		r=r.replace("u2","ù");
		r=r.replace("u3","û");
		r=r.replace("u4","ü");
		r=r.replace("i3","î");
		r=r.replace("i4","ï");
		r=r.replace("o3","ô");
		r=r.replace("c5","ç");
		return r;
	}
	
	public static String convertPhones(String s) {
		String r = "";
		int i=0;
		if (s.charAt(0)=='*') {
			// TODO: pas de liaison possible !
			i++;
		}
		for (;i<s.length();i++) {
			if (s.charAt(i)=='@') {
				r+=" [ swa ]";
			} else if (s.charAt(i)=='*') {
				// le * est normalement place au debut pour indiquer les mots sans liaisons precedentes possibles
				// mais avec les most a tirets, ce * peut aussi se retrouver en milieu de mots !
				// pour le moment on ne le traite pas, mais il faudrait y penser...
			} else if (s.charAt(i)=='o') {
				if (i<s.length()-1&&s.charAt(i+1)=='~') {
					r+=" on"; i++;
				} else
					r+=" oh";
			} else if (s.charAt(i)=='h'&&s.charAt(i+1)=='h') {
				r+=" hh";
				i++;
			} else if (s.charAt(i)=='2') {
				r+=" eu";
			} else if (s.charAt(i)=='9') {
				if (i<s.length()-1&&s.charAt(i+1)=='~') {
					r+=" in"; i++;
				} else
					r+=" euf";
			} else if (s.charAt(i)=='6') {
				r+=" swa";
			} else if (s.charAt(i)=='N') {
				r+=" n g";
			} else if (s.charAt(i)=='O'&&i<s.length()-1&&s.charAt(i+1)=='/') {
				r+=" oh"; i++;
			} else if (s.charAt(i)=='E'&&i<s.length()-1&&s.charAt(i+1)=='/') {
				r+=" eh"; i++;
			} else if (s.charAt(i)=='e'&&i<s.length()-1&&s.charAt(i+1)=='~') {
				r+=" in"; i++;
			} else if (s.charAt(i)=='a'&&i<s.length()-1&&s.charAt(i+1)=='~') {
				r+=" an"; i++;

			// phonemes optionnels
			} else if (s.charAt(i)=='(') {
				r+=" [ ";
			} else if (s.charAt(i)==')') {
				r+=" ] ";
			
			// variantes de prononciation
			} else if (s.charAt(i)=='{') {
				boolean bdlexdefined = false;
				if (s.length()>=i+5) {
					if (s.substring(i,i+5).equals("{O~n}")) {
						r+=" ( oh n | on | on n )"; i+=4; bdlexdefined=true;
					} else if (s.substring(i,i+5).equals("{TSj}")) {
						r+=" ( t S j | S j | t S )"; i+=4; bdlexdefined=true;
					} else if (s.substring(i,i+5).equals("{A~n}")) {
						r+=" ( an | a n )"; i+=4; bdlexdefined=true;
					} else if (s.substring(i,i+5).equals("{a~n}")) {
						r+=" ( an | an n | a n | in n | E n )"; i+=4; bdlexdefined=true;
					} else if (s.substring(i,i+5).equals("{dZj}")) {
						r+=" ( d Z j | Z j | d Z )"; i+=4; bdlexdefined=true;
					} else if (s.substring(i,i+5).equals("{^ks}")) {
						r+=" ( k s | g z )"; i+=4; bdlexdefined=true;
					} else if (s.substring(i,i+5).equals("{^ts}")) {
						r+=" ( t s | d z )"; i+=4; bdlexdefined=true;
					}
				}
				if (!bdlexdefined&&s.length()>=i+4) {
					if (s.substring(i,i+4).equals("{6R}")) {
						r+=" ( swa R | E R )"; i+=3; bdlexdefined=true;
					} else if (s.substring(i,i+4).equals("{6n}")) {
						r+=" ( swa n | E n )"; i+=3; bdlexdefined=true;
					} else if (s.substring(i,i+4).equals("{6~}")) {
						r+=" ( swa n | in | in n )"; i+=3; bdlexdefined=true;
					} else if (s.substring(i,i+4).equals("{Ei}")) {
						r+=" E [ j ]"; i+=3; bdlexdefined=true;
					} else if (s.substring(i,i+4).equals("{TS}")) {
						r+=" [ t ] S"; i+=3; bdlexdefined=true;
					} else if (s.substring(i,i+4).equals("{ai}")) {
						r+=" ( a j | E )"; i+=3; bdlexdefined=true;
					} else if (s.substring(i,i+4).equals("{aI}")) {
						r+=" ( a j | i )"; i+=3; bdlexdefined=true;
					} else if (s.substring(i,i+4).equals("{au}")) {
						r+=" ( a w | u )"; i+=3; bdlexdefined=true;
					} else if (s.substring(i,i+4).equals("{dZ}")) {
						r+=" [ d ] Z"; i+=3; bdlexdefined=true;
					}
				}
				if (!bdlexdefined&&s.length()>=i+3) {
					if (s.substring(i,i+3).equals("{E}")) {
						r+=" ( E | swa )"; i+=2; bdlexdefined=true;
					} else if (s.substring(i,i+3).equals("{x}")) {
						r+=" ( R | Z )"; i+=2; bdlexdefined=true;
					}
				}
				if (!bdlexdefined) {
					// dans dicoLORIA, on note ainsi les alternatives
					r+=" ( ";
				}
			} else if (s.charAt(i)=='|') {
				// dans dicoLORIA, on note ainsi les alternatives
				r+=" | ";
			} else if (s.charAt(i)=='}') {
				// dans dicoLORIA, on note ainsi les alternatives
				r+=" ) ";
			} else if (s.charAt(i)=='#') {
				// dans dicoLORIA, on note ainsi les silences
				r+=" sil ";
				
				// ci-dessous: simple phoneme
			} else r+=" "+s.charAt(i);
		}
		return r+" ";
	}

}

class Entree implements Serializable, Comparable<Entree> {
	private static final long serialVersionUID = -5569009786502786053L;
	String phonesBase="";
	String phonesOption="";
	Entree autrePossible=null;
	
	
	static enum POStag { unk,
		adv, conj, det, adjnomfem, /* adjectif ou nom feminin */
		adjnommasc, /* adjectif ou nom masculin */
		adjnom, /* adjectif ou nom masculin ou feminin */
		adj, interj, partpassé, nom, prep, pron, verb
	}
	static enum Genre {unk, 
		masc, fem, invar, neutre
	}
	static enum Nombre {unk,
		sing, plur, invar, neutre
	}
	static enum Personne {unk,
		prem, deux, trois
	}
	static enum Temps {unk,
		prés, imparf, passé, futur
	}
	static enum Mode {unk,
		indicatif, subjonctif, conditionnel, impératif, infinitif, participe
	}
	class Syntax implements Serializable, Comparable<Syntax> {
		private static final long serialVersionUID = -1825572948836274656L;
		POStag postag=POStag.unk;
		Genre genre=Genre.unk;
		Nombre nombre=Nombre.unk, nombre2=Nombre.unk;
		Personne pers=Personne.unk;
		Temps temps=Temps.unk;
		Mode mode=Mode.unk;
		String lemme=null;
		public int compareTo(Syntax s) {
			if (postag==s.postag&&genre==s.genre&&nombre==s.nombre&&nombre2==s.nombre2&&pers==s.pers&&temps==s.temps&&
					mode==s.mode) {
				if (lemme==null) {
					if (s.lemme==null) return 0;
					else return 1;
				} else if (lemme.equals(s.lemme)) return 0;
			}
			return 1;
		}
	}
	// les autres syntaxes possibles sont contenues dans "autrepossible"
	Syntax syntax = new Syntax();
	
	/**
	 * return
	 * 	0  si this est inclus dans e (this ne doit pas avoir de liste suivante)
	 *  1  si ils sont différents
	 */
	public int compareTo(Entree e) {
		if (e==null) return 1;
		if (autrePossible!=null) return 1;
		if (phonesBase.equals(e.phonesBase)&&phonesOption.equals(e.phonesOption)&&syntax.compareTo(e.syntax)==0)
			return 0;
		else return compareTo(e.autrePossible);
	}
	
	/**
	 * ecrase le 1er champ syntaxique !
	 * @param tag
	 */
	public void setPOStag(POStag tag) {
		syntax.postag=tag;
	}
	public void setGenre(Genre g) {
		syntax.genre=g;
	}
	public void setNombre(Nombre n) {
		syntax.nombre=n;
	}
	/**
	 * pour certains déterminants, voir par exemple "mes" dans BDLex
	 * @param n
	 */
	public void setNombre2(Nombre n) {
		syntax.nombre2=n;
	}
	public void setPersonne(Personne p) {
		syntax.pers=p;
	}
	public void setTemps(Temps t) {
		syntax.temps=t;
	}
	public void setMode(Mode m) {
		syntax.mode=m;
	}
	public void setLemme(String l) {
		syntax.lemme=""+l;
	}
	
	/**
	 * return false ssi this est inclus dans e
	 * @param e est toujours le plus complet (l'ancien) !
	 */
	public boolean combinerAvec(Entree e) {
		if (compareTo(e)!=0) {
			autrePossible=e;
			return true;
		} else return false;
	}
}
