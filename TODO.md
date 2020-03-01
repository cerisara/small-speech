But = faire un system de reco simple en quelques mots isolés

- construire un grand corpus en enregistrant du bruit de fond (beaucoup), et en générant des dizaines de version de chaque mot avec MaryTTS en changeant légèrement les paramètres, puis en ajoutant ces mots au bruit de fond
- construire un modèle DL simple en python:
  - input = 2x seqs de MFCC pour 2 occs (motA et motB) du corpus
  - quelques couches de convolution
  - seq embeddings avec 2-matrices self-attention (à la CVDD) pour avoir un embedding unique pour la séquence
  - ajouter quelques couches linear + relu
  - terminer par 2 neurones: meme mot, ou différent
- apprendre ce modèle sur mon corpus en samplant des mots identiques (mais avec des options différentes, des bruits de fond différents) et différents
- tester ses perfs
- rééecrire la passe forward en java

Appli:
- demander au user d'enregistrer 1x occ des mots à reconnaître
- reconnaitre en comparant avec toutes ces occs de train

