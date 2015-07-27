er-visualizer
=============

D3 and Play based visualization for entity-relation graphs, especially for NLP and information extraction

# Basic Example

Here we are going to show the visualization with a few entities and relations, although it can handle upto hundreds of thousands of entities and relations.

The first page show a simple search box to identify subsets of documents to visualize.

![Search page](https://github.com/sameersingh/er-visualizer/raw/master/docs/img/search.png)

The visualization lays out all the extracted entities and relations onto a map as a graph. The entity nodes are sized according to their popularity (in the document collection), and colored according to their types (person, location, or organization).

Clicking on an entity node bring ups details from Freebase on the left, and detailed textual provenance on the right. The provenance also contained fine-grained types, if part of the annotations.

![Entities](https://github.com/sameersingh/er-visualizer/raw/master/docs/img/entity.png)

The edges represent extracted relations, with the width proportional to the number of mentions of the relation. Clicking on a relation brings up their provenances on the right.

![Relations](https://github.com/sameersingh/er-visualizer/raw/master/docs/img/relations.png)

## Running the Example

The following are the instructions for running the basic example shown above

1. `sbt clean compile`
1. `sbt run`
1. Open [localhost:9000](http://localhost:9000/)
1. Use `obama` to visualize ~~all~~ both documents.

# Input Data

To visualize the documents, they needed to be annotated with basic NLP (NER specifically), linked to Freebase entities, and have relation extracted on a per-sentence level. The following are the list of files that contain this information.

For the files used for the visualization above, see [data/test](https://github.com/sameersingh/er-visualizer/tree/master/data/test).

## Necessary files

1. Create a directory where all the files below will go, and specify it in `application.conf` as `nlp.data.baseDir` (See `reference.conf`)
1. **Documents**: A json file (`docs.json.gz`), as described below (see **Processed Documents**), containing the processed documents with entity linking and relations.
1. **Entities**: Information about the entities from Freebase, either read from a Mongo server, or read from files `ent.info`, `ent.freebase`, and `ent.head` as prepared from Freebase below (see **Freebase Information**)
1. `wcounts.txt.gz` and `ecounts.txt.gz`: Gzipped files containing list of keywords and entities for search (generated from `docs.json.gz` using `org.sameersingh.ervisualizer.data.WordCounts`).

## Processed Documents

This will describe how we generate `docs.json.gz` (file name can me modified in the configuration using `docsFile`).

We will be using `nlp_serde` as the underlying document representation. The library contains data structures for representing most of the NLP annotations, including entity linking and relation extraction, so you can directly wrap your document annotations into those classes, and then write out a documents file using `nlp_serde.writers.PerLineJsonWriter`. See [`org.sameersingh.ervisualizer.data.TestDocs`](https://github.com/sameersingh/er-visualizer/blob/master/app/org/sameersingh/ervisualizer/data/TestDocs.scala) for example annotated documents.

Or, less desirably, you can write out the JSON files directly from your code (see `data/test/docs.json.gz` for an example).

## Freebase Information

Visualization needs access to Freebase information about the entities that appear in your document collection.

You can either have a Mongo server running (requires a lot of memory, and might be slower), or create the relevant files yourself (configured using `nlp.data.mongo` flag). The test above uses the file mode, i.e. you don't need to run a Mongo server.

### Reading Freebase Info from Mongo

1. Download a [freebase RDF dump](http://commondatastorage.googleapis.com/freebase-public/rdf/freebase-rdf-latest.gz), for example `freebase-rdf-2014-07-06-00-00.gz`.
1. Grep the dump to create a file for each of the following relations (using something like `zcat freebase-rdf-2014-07-06-00-00.gz | grep "<http://rdf.freebase.com/ns/$relation>" | gzip > $relation.gz`):
  - `type.object.id`
  - `type.object.name`
  - `common.topic.image`
  - `common.topic.description`
  - `common.topic.notable_types`
  - `location.location.geolocation`
  - `location.geocode.longitude`
  - `location.geocode.latitude`
1. Start a Mongo server, and run `org.sameersingh.ervisualizer.freebase.LoadMongo` to populate it (change `baseDir`, `host`, and `port` if needed)
1. Run visualization with `nlp.data.mongo = true` to use the Mongo server.

### Reading Freebase Info from Files

Reading Mongo can be inefficient, and thus it is more efficient to read this information directly from files, as we will describe here. Note that you still need Mongo to generate the files the first time around, but you don't need it after the files have been created. 

The files `ent.info`, `ent.freebase`, and `ent.head` are pretty simple per-line JSON files containing the entity information, corresponding to the case classes in [`Entity.scala`](https://github.com/sameersingh/er-visualizer/blob/master/app/org/sameersingh/ervisualizer/data/Entity.scala). You can use the method below to construct these files, or generate your own directly. The only constraint is that these three files are aligned, i.e. information about the same entity appears in the three files on the same line number.

If you want to use Mongo to generate these files:

1. Previous steps of creating documents and setting up a Mongo server.
1. Run `org.sameersingh.ervisualizer.freebase.GenerateEntInfo` to generate the files.
1. Run visualization with `nlp.data.mongo = false`, and you can shut down the Mongo sever.

# Contact

Please use Github issues if you have problems/questions.
