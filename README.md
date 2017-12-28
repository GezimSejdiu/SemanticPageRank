# Semantic PageRank
> Towards a Semantic PageRank for Ranking Web Search Results.

## Description
One of the pivotal steps within the semantic search endeavors is to improve ranking of Web Pages by exploiting semantic their description hyperlinks.
Adopting ontologies to model semantics for Web Page ranking is not a novelty.
There are works which apply variations of PageRank or other authority-based algorithms over ontologies for semantic ranking of resources on the Web.
In this paper, we propose an approach which does both modeling and analysis of semantics of Web pages with their links for ranking by making outright use of the Semantic Web potential.
We introduce an ontology, the SemanticRankOnto to model Web Pages with their hyperlinks.
Furthermore, to calculate the PageRank values we defined several rules using Semantic Web Rule Language (SWRL).
The preliminary evaluation of the proposed approach is conducted over a Wikipedia dump of pages.

 <span>Gezim Sejdiu</span>, "Semantic Ranking of Web Pages : The Wikipedia Case Study,"; Master Thesis, University of Prishtina, Kosova, 2014. [[Download PDF]](https://www.researchgate.net/profile/Gezim_Sejdiu/publication/264400068_Rangimi_semantik_i_ueb_faqeve_-_Wikipedia_si_nje_rast_studimi_Semantic_Ranking_of_Web_Pages_-_The_Wikipedia_Case_Study/links/569904a808aeeea98594506c/Rangimi-semantik-i-ueb-faqeve-Wikipedia-si-nje-rast-studimi-Semantic-Ranking-of-Web-Pages-The-Wikipedia-Case-Study.pdf?origin=publication_detail&amp;ev=pub_int_prw_xdl&amp;msrp=AA37FwBzmKERYXi1M2vhWudDort1uLpVM1OSeZjP0qQ0IpEmuvefoRBnX2gTOpctGw5NQ-WolOCmQ4CYW6PwSE9UP27VAGvrmWbzGO7X5ssHhngO5v4.lVzcwbIYCwbOaWUUPbOVaMXxWfjqqco8y7lPka6Sx7akCcIJgNaBUsRP9ybuqT0wg-ngpyu_fSPRrs63hkYjLJvJZvNDWR3fzZopSg.2puAeXufSna9VfnNYPTr3-L_fgans7XuC2YL1uo73vNE68nlRwKz0sc_RvUZusuNMkwxtSkJClAIrpmtZNrOeB7UtJ9-xaG5j8pqRQ.jB1XguS-PfblCV77SV_zZJK2kMl5WXGMPP-NgQs8X5x0efgfCk_urpyJJb-cnp7LHUlXEUiq_t5wSdDgb3j9lXd99NTG_tyV6LESEQ)
<pre><code >@MastersThesis{sejdiu2014,
Title = {Semantic {R}anking of {W}eb {P}ages : {T}he {W}ikipedia {C}ase {S}tudy},
Author = {Gezim Sejdiu},
School = {Faculty of {E}lectrical and {C}omputer {E}ngineering},
Year = {2014},
Address = {University of Prishtina, Kosova},
Month = {7},
Keywords = {2014 sejdiu},
}</code></pre>

## Structure
### [OwlProject](https://github.com/gezims/SemanticPageRank/tree/master/OwlProject)
Contains the Semantic PageRank rules and their calculations.

### [SemanticSearchWikiApp](https://github.com/gezims/SemanticPageRank/tree/master/SemanticSearchWikiApp)
Contains the Semantic Wiki Search over precomputed rank values.