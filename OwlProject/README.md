# Owl Project
> The Semantic Web rules to calculate the PageRank.

## Description
Rules to calculate the semantic PageRank, but for ranking authors in co-authorship networks, have already been defined in [Ranking Authors on the Web: A Semantic AuthorRank](https://link.springer.com/chapter/10.1007%2F978-3-7091-1797-2_2).
In order for these rules to rather perform for semantic ranking of Web pages, they are expressed in conformity with the ontological constructs of the *SemanticRankOnto* ontology exclusively introduced to support pages ranking.

## SWRL rules description
### Rule 1 : Total number of Websites
Calculates the value of the global totalNrWeb-Sites property, i.e., the total number of Web-Site instances in the ontology.
```
portal:Person(?a) ◦ sqwrl:makeSet(?s,?a) ◦ sqwrl:size(?n,?s) → totalNrAuthors(Constant1,?n)
```
It first finds all pages ?w and stores them in a set ?s (the makeSet built-in function), whose total number of elements is counted (the size function) and stored as the value ?n of the totalNrWeb-Sites property of the instance Constant1 in the class Constants within the ontology.

### Rule 2 : Total number of outgoing links of a given Website
Calculates the value of the hasTotalNrOutLinks property, i.e., the number of outgoing links for each given Web-Site.
```
portal:Web-Site(?w) ∧ hasOutLinks(?w, ?a) ◦ sqwrl:makeSet(?s, ?a) ∧ sqwrl:groupBy(?s, ?w) ◦ sqwrl:size(?n, ?s) → hasTotalNrOutLinks(?w, ?n)
```
Similar to Rule 1, it first retrieves all Web pages ?w, and its outgoing links ?a, storing them into a set ?s and grouping them (the built-in function groupBy) by pages. At the end, it counts the number of outgoing links within the given group in the set (i.e., per page), and stores that number as the value ?n of the hasTotalNrOutLinks property of the instance ?w in the class portal:Web-Site within the ontology.

### Rule 3 : Initial PageRank - the initial rank of a given Website
Calculates the initial PageRank for each individual Web-Site, enabling thus the iterative calculation of PageRank.
```
hasOutLinks(?wj, ?wrel) ∧ wiki:hasOutLinkValue(?wrel, ?wi) ∧ hasTotalNrOutLinks(?wj, ?cj) ∧ totalNrWeb-Sites(Constant1, ?n) ∧ swrlm:eval(?yj, ”1/cj”, ?cj) ◦ sqwrl:makeBag(?s, ?yj) ∧ qwrl:groupBy(?s, ?wi) ◦ sqwrl:sum(?ally, ?s) ∧ swrlm:eval(?pri, ”(0.15 / n) + 0.85 * ally”, ?n, ?ally) → hasWebPR(?wi, ?pri)
```
This rule proceeds, for each given Web-Site ?wj, as follows:
  1. It first considers all links going out of page ?wj, i.e., all Web-SiteLinks instances ?wrel which link page ?wj to page ?wi.
  2. Retrieves and calculates (the eval built-in function of the swrlm math library of SWRL) few values required later in the rule: the number ?cj of outgoing links from page ?wj (Rule 2), the ratio ?yj = 1/?cj of these links, then the total number ?n of pages in the ontology (Rule 1).
  3. Calculates the sum ?ally of all outgoing links within bag ?s which point to page ?wi (see grouping by ?wi within this bag).
  4. Finaly, it calculates (the eval function) the PageRank value ?pri of page ?wi, taking into account the sum ?ally, but also the dumping factor equal to 0.85, as well as the total number ?n of pages in the ontology, and stores the obtained value ?pri to the hasWebPR property of the ?wi instance of the portal:Web-Site class in the ontology.

### Rule 4 : PageRank - the rank of a given Website
Once Rule 3 is evaluated, the system is then able to evaluate the main rule, Rule 4, which actually provides the semantic PageRank values of pages.
```
hasWebPR(?wj, ?prj) ∧ hasOutLinks(?wj, ?wrel) ∧ wiki:hasOutLinkValue(?wrel, ?wi) ∧ hasTotalNrOutLinks(?wj, ?cj) ∧ totalNrWeb-Sites(Constant1, ?n) ∧ swrlm:eval(?yj, ”prj/cj”, ?prj, ?cj) ◦ sqwrl:makeBag(?s, ?yj) ∧ sqwrl:groupBy(?s, ?wi) ◦ sqwrl:sum(?ally, ?s) ∧ swrlm:eval(?pri, ”0.15 / n + 0.85 * ally”, ?n, ?ally) → hasWebPR(?wi, ?pri)
```
The only difference of this rule with the initial PageRank rule (Rule 3) is in step
2:

  - The contributions of all backlinking pages ?wj to the page under consideration, e.g., to page ?wi, involve also their actual PageRank values. In other words, the contribution of the backlinking page ?wj is ?yj = prj/?cj, where prj is the actual PageRank value of page ?wj stored as its hasWebPR(?wj, ?prj) property, whereas ?cj is the total number of links going out of page ?wj. At the first iteration of this rule, the PageRank values ?prj at the body of the rule are those inferred by the initial PageRank rule.

This rule will iterate once over all pages and the pages they point to through the outgoing links found in the ontology (?wrel individuals of the Web-SiteLinks class), compute their actual PageRank values, and accordingly assign them to the hasWebPR property which holds the PageRank value of a Web page.
