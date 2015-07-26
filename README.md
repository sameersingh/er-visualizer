er-visualizer
=============

D3 and Play based visualization for entity-relation graphs, especially for NLP and information extraction

# Screenshot

# Running Test Example

# Input

The following are the list of files needed for the visualization.

Necessary files:
1. Create a directory where all the files below will go, and specify it in `application.conf` as `nlp.data.baseDir` (See `reference.conf`)
1. **Documents**: A json file (`docs.json.gz`), as described below (see `Processed Documents`), containing the processed documents with entity linking and relations.
1. **Entities**: `ent.info`, `ent.freebase`, and `ent.head` as prepared from Freebase below (see `Freebase Information`)

Other generated files:
1. `wcounts.txt.gz`: Gzipped file containing list of keywords for search (generated using `org.sameersingh.ervisualizer.data.WordCounts`).
1. `ecounts.txt.gz`: Gzipped file containing list of entities for search (generated using `org.sameersingh.ervisualizer.data.WordCounts`).

## Processed Documents

This will describe how we generate `docs.json.gz` (file name can me modified in the configuration using `docsFile`).

We will be using `nlp_serde` as the underlying document representation. The library contains data structures for representing most of the NLP annotations, including entity linking and relation extraction, so you can directly wrap your document annotations into those classes, and then write out a documents file using `nlp_serde.writers.PerLineJsonWriter`. Or, less desirably, you can write out the JSON files directly from your code (see `data/test/docs.json.gz` for an example).

## Freebase Information

The files `ent.info`, `ent.freebase`, and `ent.head` and pretty simple per-line JSON files containing the entity information displayed in the visualization, corresponding to the case classes in `org/sameersingh/ervisualizer/data/Entity.scala`. You can use the method below to construct these files, or use your own. The only constraint is that these three files are aligned, i.e. information about the same entity appears in the three files on the same line number.

This will describe how the files `ent.info`, `ent.freebase`, and `ent.head` will be created. Note that this needs previous step of creating documents to be complete.

1. Download a freebase RDF dump, we will use `freebase-rdf-2014-07-06-00-00.gz`.
1. Grep the dump to create a file for each of the following relations (using something like `zcat freebase-rdf-2014-07-06-00-00.gz | grep "<http://rdf.freebase.com/ns/$relation>" | gzip > $relation.gz`):
  - `type.object.id.gz`
  - `type.object.name.gz`
  - `common.topic.image.gz`
  - `common.topic.description.gz`
  - `common.topic.notable_types.gz`
  - `location.location.geolocation.gz`
  - `location.geocode.longitude.gz`
  - `location.geocode.latitude.gz`
1. Start a Mongo server, and run `org.sameersingh.ervisualizer.freebase.LoadMongo` to populate it (change `baseDir`, `host`, and `port` accordingly)
1. Run visualization with `nlp.data.mongo = true`, which will write out the `ent.info`, `ent.freebase`, and `ent.head` files. For subsequent execution, you don't need the Mongo server, and can set `nlp.data.mongo = false`.

