# Efficient Query Execution over Large Databases Through Semantic Caching of Bitmap Indices

Researches the efficiency of querying large database systems by leveraging a semantic cache over a bitmap index.
Program is written in C and tests various cache lookup algorithms and their effect on preformance. Program was later translated into Java and research was continued (results not included on poster).  



# Research Poster
I presented this poster at the University of Puget Sound Research Symposium in September 2019. This poster demonstates the early results of testing the semantic cache and demonstrates how it is being leveraged over the bitmap index. My advisor and I continuted to research this topic after this poster was created, so this poster does not include our more recent developments. Note that this poster was meant for people with little to no knowledge of databases or computer science. 
![Poster](https://github.com/smcclain56/semanticCache/blob/master/images/poster.png)

## Background of the Bitmap Index and Semantic Cache
This section introduces the problem of increasingly data intensive applications and the need for more efficient data structures to query those large-scale data stores. It then gives a short description of a bitmap index and a semantic cache. 
![Background](https://github.com/smcclain56/semanticCache/blob/master/images/background.png)

## Mechanism of Semantic Cache
This section explains how the semantic cache works using an example SQL query and walking through the whole process using images. It shows how the bitmap index, database and semantic cache would all work together to produce the results efficiently. It also gives a visual on how the recursive algorithm works on the cache. 
![Mechanism](https://github.com/smcclain56/semanticCache/blob/master/images/mechanism.png)

## Explanation of Query Files Used
This section explains one of the fundamental aspects of computer science, boolean logic. It shows a short explanation of how the "AND" and "OR" operators work in the context of bitmap indices. It then explains what point and range queries are, in order for the readers to better understand what the plots below mean.  
![Query File](https://github.com/smcclain56/semanticCache/blob/master/images/queryFile.png)

## Recursive Algorithm Used to Search the Semantic Cache
This section shows one of the alorithms we tested on the semantic cache. This algorithm uses recursion to continuously search the cache for as much of the query as it can find. The cache was implemented using a hashmap where all the queries with the same starting column are hashed in the same bucket and their various end columns are then chained together. This algorithm takes advantage of the hashmap structure by hashing into the bucket of the start column, finding the best match in that list and then recursively hashing from the ending value to try to build the entire query. 
![Algorithm](https://github.com/smcclain56/semanticCache/blob/master/images/algorithm.png)

## Plots Demonstrating Preformance Gains
This section shows four plots that compare the results of executing a query file using the semantic cache and not using a cache. The three bar charts show how performance is effected by the number of point versus range queries in the query file. We expected the cache to more important the more range queries there were, which is what the results showed. The last plot shows the speedup as the query file gets larger.  
![Plots](https://github.com/smcclain56/semanticCache/blob/master/images/plots.png)

## Results Summary
This last section summarizes the results we learned from the plots. The results were as predicted. The semantic cache had a large influence on performance, especially as the percentage of range queries increased and the query file grew. 
![Results](https://github.com/smcclain56/semanticCache/blob/master/images/results.png)

# Acknowledgments
* David Chiu - Faculty Advisor - For his time and support as he guided me throughout the course of this project.
* University of Puget Sound - For providing me the opportunity to complete this project as well as the resources to print and present my research



