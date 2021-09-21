# LS2

This repository has the code of Local  Search  and  LanguageSimplifier (LS2), presented in the paper https://ojs.aaai.org/index.php/AAAI/article/view/16114 presented in the Proceedings of the AAAI Conference on Artificial Intelligence AAAI 2021.

How to Run:

-This code needs you to import an external code, which includes the mRTS code, the testbed of our method. The code is available here:  https://github.com/julianmarino/mRTS_Compiler_LS2  

-Run the class RunTests_LS2.java
- Before running, chose the parameter withLasi=False if you want to disable the Domain Specific Language Simplification Step (Lasi) and just run the local search. If you set that parameter as true, a search-based algorithm will play a game before the local search starts.
- The resulting script is printed in the file best.txt
