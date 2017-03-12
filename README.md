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

## Structure
### [OwlProject](https://github.com/gezims/SemanticPageRank/tree/master/OwlProject)
Contains the Semantic PageRank rules and their calculations.

### [SemanticSearchWikiApp](https://github.com/gezims/SemanticPageRank/tree/master/SemanticSearchWikiApp)
Contains the Semantic Wiki Search over precomputed rank values.
