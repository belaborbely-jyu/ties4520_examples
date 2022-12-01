/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import info.sswap.api.input.Input;
import info.sswap.api.input.Inputs;
import info.sswap.api.input.io.JenaSerializer;
import info.sswap.api.input.io.SSWAPIndividualDeserializer;
import info.sswap.api.model.RDG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPType;

import java.net.URI;

import org.json.JSONObject;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Simple test program for manual testing of inputs
 * 
 * @author Evren Sirin
 */
public class InputTestDriver {

	static String rdf=
"{\n" + 
"  \"inputs\": [\n" + 
"    {\n" + 
"      \"inputs\": [\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"string\",\n" + 
"            \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"          },\n" + 
"          \"description\": \"The WGS84 longitude of a SpatialThing (decimal degrees).\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/spatialthing/longitude\",\n" + 
"          \"label\": \"Longitude\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"string\",\n" + 
"            \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"          },\n" + 
"          \"description\": \"Whether An associated TreeGenes TreeSample is genotyped\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/treesample/genotyped\",\n" + 
"          \"label\": \"Genotyped\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"string\",\n" + 
"            \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"          },\n" + 
"          \"description\": \"An elevation associated with a TreeGenes Spatial Thing\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/spatialthing/elevation\",\n" + 
"          \"label\": \"Elevation\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 1,\n" + 
"          \"max\": 1,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"string\",\n" + 
"            \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"          },\n" + 
"          \"description\": \"A identifier for a TreeGenes TreeSample\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/treesample/id\",\n" + 
"          \"label\": \"TreeSample Identifier\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"string\",\n" + 
"            \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"          },\n" + 
"          \"description\": \"The WGS84 latitude of a SpatialThing (decimal degrees)\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/spatialthing/latitude\",\n" + 
"          \"label\": \"Latitude\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        }\n" + 
"      ],\n" + 
"      \"description\": \"TreeGenes notion of a TreeSample .\",\n" + 
"      \"label\": \"Tree Sample\",\n" + 
"      \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"    },\n" + 
"    {\n" + 
"      \"inputs\": [\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"inputs\": [\n" + 
"              {\n" + 
"                \"label\": \"ForwardPrimer\",\n" + 
"                \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/ForwardPrimer\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"label\": \"Primer\",\n" + 
"                \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/Primer\"\n" + 
"              }\n" + 
"            ],\n" + 
"            \"label\": \"\",\n" + 
"            \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has a Forward Primer record for a TreeGenes record\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/primer/hasForwardPrimer\",\n" + 
"          \"label\": \"has Forward Primer\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"EST\",\n" + 
"            \"type\": \"http://sswapmeet.sswap.info/treeGenes/est/EST\"\n" + 
"          },\n" + 
"          \"description\": \"A property which hasEST record for a TreeGenes contig;\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/est/hasEST\",\n" + 
"          \"label\": \"has EST\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"inputs\": [\n" + 
"              {\n" + 
"                \"label\": \"OBO\",\n" + 
"                \"type\": \"http://sswapmeet.sswap.info/OBO/OBO\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"label\": \"Term\",\n" + 
"                \"type\": \"http://sswapmeet.sswap.info/OBO/Term\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"label\": \"GO\",\n" + 
"                \"type\": \"http://sswapmeet.sswap.info/OBO/GO\"\n" + 
"              }\n" + 
"            ],\n" + 
"            \"label\": \"\",\n" + 
"            \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"          },\n" + 
"          \"description\": \"Standardized predicate for any \\\"thing\\\" to assert a relationship  to something of type obo:GO.\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/OBO/hasGO\",\n" + 
"          \"label\": \"hasGO\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 1,\n" + 
"          \"max\": 1,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"string\",\n" + 
"            \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"          },\n" + 
"          \"description\": \"A unique ID for a TreeGenes Contig; e.g., 9564\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/contig/id\",\n" + 
"          \"label\": \"Contig ID\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"inputs\": [\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"SNP\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/snp/SNP\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has a SNP record\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/snp/hasSNP\",\n" + 
"                \"label\": \"has SNP\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 1,\n" + 
"                \"max\": 1,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"string\",\n" + 
"                  \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"                },\n" + 
"                \"description\": \"A unique Id for a TreeGenes Alignment\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/alignment/id\",\n" + 
"                \"label\": \"Alignment Id\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"integer\",\n" + 
"                  \"type\": \"http://www.w3.org/2001/XMLSchema#integer\"\n" + 
"                },\n" + 
"                \"description\": \"The length of a TreeGenes Alignment\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/alignment/length\",\n" + 
"                \"label\": \"Alignment length\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"SeqChromat\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/seqchromat/SeqChromat\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has Sequencing Chromatogram record for a TreeGenes record.\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/seqchromat/hasSeqChromat\",\n" + 
"                \"label\": \"has Seq Chromat\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"inputs\": [\n" + 
"                    {\n" + 
"                      \"min\": 0,\n" + 
"                      \"max\": 2147483647,\n" + 
"                      \"range\": {\n" + 
"                        \"label\": \"Sequence\",\n" + 
"                        \"type\": \"http://sswapmeet.sswap.info/treeGenes/sequence/Sequence\"\n" + 
"                      },\n" + 
"                      \"description\": \"A property which has a Sequence record for a TreeGenes record.\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/treeGenes/sequence/hasSequence\",\n" + 
"                      \"label\": \"has Sequence\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"min\": 1,\n" + 
"                      \"max\": 1,\n" + 
"                      \"range\": {\n" + 
"                        \"label\": \"string\",\n" + 
"                        \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"                      },\n" + 
"                      \"description\": \"A unique ID for a TreeGenes Amplicon; e.g., 0_8156_01\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/id\",\n" + 
"                      \"label\": \"Amplicon ID\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"min\": 0,\n" + 
"                      \"max\": 2147483647,\n" + 
"                      \"range\": {\n" + 
"                        \"label\": \"Contig\",\n" + 
"                        \"type\": \"http://sswapmeet.sswap.info/treeGenes/contig/Contig\"\n" + 
"                      },\n" + 
"                      \"description\": \"A property which has a Contig record for a TreeGenes record\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/treeGenes/contig/hasContig\",\n" + 
"                      \"label\": \"has Contig\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    }\n" + 
"                  ],\n" + 
"                  \"label\": \"\",\n" + 
"                  \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has an Amplicon record for a TreeGenes contig\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/hasAmplicon\",\n" + 
"                \"label\": \"has Amplicon\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"inputs\": [\n" + 
"                    {\n" + 
"                      \"label\": \"DataFormat\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/data/DataFormat\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"label\": \"Sequence\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/sequence/Sequence\"\n" + 
"                    }\n" + 
"                  ],\n" + 
"                  \"label\": \"\",\n" + 
"                  \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                },\n" + 
"                \"description\": \"A predicate allowing any \\\"thing\\\" to assert a relationship to a Sequence object.\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/sequence/hasSequence\",\n" + 
"                \"label\": \"has Sequence\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              }\n" + 
"            ],\n" + 
"            \"label\": \"\",\n" + 
"            \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has an Alignment record for a TreeGenes record.\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/alignment/hasAlignment\",\n" + 
"          \"label\": \"has Alignment\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"inputs\": [\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"SNP\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/snp/SNP\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has a SNP record\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/snp/hasSNP\",\n" + 
"                \"label\": \"has SNP\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"Sequence\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/sequence/Sequence\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has a Sequence record for a TreeGenes record.\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/sequence/hasSequence\",\n" + 
"                \"label\": \"has Sequence\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 1,\n" + 
"                \"max\": 1,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"string\",\n" + 
"                  \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"                },\n" + 
"                \"description\": \"A unique ID for a TreeGenes Amplicon; e.g., 0_8156_01\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/id\",\n" + 
"                \"label\": \"Amplicon ID\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"Contig\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/contig/Contig\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has a Contig record for a TreeGenes record\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/contig/hasContig\",\n" + 
"                \"label\": \"has Contig\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              }\n" + 
"            ],\n" + 
"            \"label\": \"\",\n" + 
"            \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has an Amplicon record for a TreeGenes contig\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/hasAmplicon\",\n" + 
"          \"label\": \"has Amplicon\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"inputs\": [\n" + 
"              {\n" + 
"                \"label\": \"ReversePrimer\",\n" + 
"                \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/ReversePrimer\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"label\": \"Primer\",\n" + 
"                \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/Primer\"\n" + 
"              }\n" + 
"            ],\n" + 
"            \"label\": \"\",\n" + 
"            \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has a Reverse Primer record for a TreeGenes record\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/primer/hasReversePrimer\",\n" + 
"          \"label\": \"has Reverse Primer\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"Taxa\",\n" + 
"            \"type\": \"http://sswapmeet.sswap.info/taxa/Taxa\"\n" + 
"          },\n" + 
"          \"description\": \"Standardized predicate for any \\\"thing\\\" to assert a relationship to a Taxa\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/taxa/hasTaxa\",\n" + 
"          \"label\": \"hasTaxa\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"Expasy\",\n" + 
"            \"type\": \"http://sswapmeet.sswap.info/treeGenes/expasy/Expasy\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has an ExPASy Enzyme  record for a TreeGenes contig\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/expasy/hasExpasy\",\n" + 
"          \"label\": \"has ExPASy\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        }\n" + 
"      ],\n" + 
"      \"description\": \"TreeGenes' notion of a Contig.\",\n" + 
"      \"label\": \"Contig\",\n" + 
"      \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"    },\n" + 
"    {\n" + 
"      \"inputs\": [\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"SNP\",\n" + 
"            \"type\": \"http://sswapmeet.sswap.info/treeGenes/snp/SNP\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has a SNP record\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/snp/hasSNP\",\n" + 
"          \"label\": \"has SNP\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"inputs\": [\n" + 
"              {\n" + 
"                \"min\": 1,\n" + 
"                \"max\": 1,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"string\",\n" + 
"                  \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"                },\n" + 
"                \"description\": \"A unique Id for a TreeGenes Alignment\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/alignment/id\",\n" + 
"                \"label\": \"Alignment Id\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"integer\",\n" + 
"                  \"type\": \"http://www.w3.org/2001/XMLSchema#integer\"\n" + 
"                },\n" + 
"                \"description\": \"The length of a TreeGenes Alignment\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/alignment/length\",\n" + 
"                \"label\": \"Alignment length\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"SeqChromat\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/seqchromat/SeqChromat\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has Sequencing Chromatogram record for a TreeGenes record.\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/seqchromat/hasSeqChromat\",\n" + 
"                \"label\": \"has Seq Chromat\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"Amplicon\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/Amplicon\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has an Amplicon record for a TreeGenes contig\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/hasAmplicon\",\n" + 
"                \"label\": \"has Amplicon\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"inputs\": [\n" + 
"                    {\n" + 
"                      \"label\": \"DataFormat\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/data/DataFormat\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"label\": \"Sequence\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/sequence/Sequence\"\n" + 
"                    }\n" + 
"                  ],\n" + 
"                  \"label\": \"\",\n" + 
"                  \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                },\n" + 
"                \"description\": \"A predicate allowing any \\\"thing\\\" to assert a relationship to a Sequence object.\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/sequence/hasSequence\",\n" + 
"                \"label\": \"has Sequence\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              }\n" + 
"            ],\n" + 
"            \"label\": \"\",\n" + 
"            \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has an Alignment record for a TreeGenes record.\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/alignment/hasAlignment\",\n" + 
"          \"label\": \"has Alignment\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"Sequence\",\n" + 
"            \"type\": \"http://sswapmeet.sswap.info/treeGenes/sequence/Sequence\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has a Sequence record for a TreeGenes record.\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/sequence/hasSequence\",\n" + 
"          \"label\": \"has Sequence\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 1,\n" + 
"          \"max\": 1,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"string\",\n" + 
"            \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"          },\n" + 
"          \"description\": \"A unique ID for a TreeGenes Amplicon; e.g., 0_8156_01\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/id\",\n" + 
"          \"label\": \"Amplicon ID\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"inputs\": [\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"inputs\": [\n" + 
"                    {\n" + 
"                      \"label\": \"ForwardPrimer\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/ForwardPrimer\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"label\": \"Primer\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/Primer\"\n" + 
"                    }\n" + 
"                  ],\n" + 
"                  \"label\": \"\",\n" + 
"                  \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has a Forward Primer record for a TreeGenes record\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/primer/hasForwardPrimer\",\n" + 
"                \"label\": \"has Forward Primer\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"EST\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/est/EST\"\n" + 
"                },\n" + 
"                \"description\": \"A property which hasEST record for a TreeGenes contig;\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/est/hasEST\",\n" + 
"                \"label\": \"has EST\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"inputs\": [\n" + 
"                    {\n" + 
"                      \"label\": \"OBO\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/OBO/OBO\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"label\": \"Term\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/OBO/Term\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"label\": \"GO\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/OBO/GO\"\n" + 
"                    }\n" + 
"                  ],\n" + 
"                  \"label\": \"\",\n" + 
"                  \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                },\n" + 
"                \"description\": \"Standardized predicate for any \\\"thing\\\" to assert a relationship  to something of type obo:GO.\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/OBO/hasGO\",\n" + 
"                \"label\": \"hasGO\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 1,\n" + 
"                \"max\": 1,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"string\",\n" + 
"                  \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"                },\n" + 
"                \"description\": \"A unique ID for a TreeGenes Contig; e.g., 9564\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/contig/id\",\n" + 
"                \"label\": \"Contig ID\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"Amplicon\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/Amplicon\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has an Amplicon record for a TreeGenes contig\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/hasAmplicon\",\n" + 
"                \"label\": \"has Amplicon\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"inputs\": [\n" + 
"                    {\n" + 
"                      \"label\": \"ReversePrimer\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/ReversePrimer\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"label\": \"Primer\",\n" + 
"                      \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/Primer\"\n" + 
"                    }\n" + 
"                  ],\n" + 
"                  \"label\": \"\",\n" + 
"                  \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has a Reverse Primer record for a TreeGenes record\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/primer/hasReversePrimer\",\n" + 
"                \"label\": \"has Reverse Primer\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"Taxa\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/taxa/Taxa\"\n" + 
"                },\n" + 
"                \"description\": \"Standardized predicate for any \\\"thing\\\" to assert a relationship to a Taxa\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/taxa/hasTaxa\",\n" + 
"                \"label\": \"hasTaxa\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"Expasy\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/expasy/Expasy\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has an ExPASy Enzyme  record for a TreeGenes contig\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/expasy/hasExpasy\",\n" + 
"                \"label\": \"has ExPASy\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              }\n" + 
"            ],\n" + 
"            \"label\": \"\",\n" + 
"            \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has a Contig record for a TreeGenes record\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/contig/hasContig\",\n" + 
"          \"label\": \"has Contig\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        }\n" + 
"      ],\n" + 
"      \"description\": \"TreeGenes notion of an Amplicon .\",\n" + 
"      \"label\": \"Amplicon\",\n" + 
"      \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"    },\n" + 
"    {\n" + 
"      \"inputs\": [\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"SNP\",\n" + 
"            \"type\": \"http://sswapmeet.sswap.info/treeGenes/snp/SNP\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has a SNP record\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/snp/hasSNP\",\n" + 
"          \"label\": \"has SNP\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 1,\n" + 
"          \"max\": 1,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"string\",\n" + 
"            \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"          },\n" + 
"          \"description\": \"A unique Id for a TreeGenes Alignment\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/alignment/id\",\n" + 
"          \"label\": \"Alignment Id\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"integer\",\n" + 
"            \"type\": \"http://www.w3.org/2001/XMLSchema#integer\"\n" + 
"          },\n" + 
"          \"description\": \"The length of a TreeGenes Alignment\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/alignment/length\",\n" + 
"          \"label\": \"Alignment length\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"label\": \"SeqChromat\",\n" + 
"            \"type\": \"http://sswapmeet.sswap.info/treeGenes/seqchromat/SeqChromat\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has Sequencing Chromatogram record for a TreeGenes record.\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/seqchromat/hasSeqChromat\",\n" + 
"          \"label\": \"has Seq Chromat\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"inputs\": [\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"Alignment\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/alignment/Alignment\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has an Alignment record for a TreeGenes record.\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/alignment/hasAlignment\",\n" + 
"                \"label\": \"has Alignment\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"Sequence\",\n" + 
"                  \"type\": \"http://sswapmeet.sswap.info/treeGenes/sequence/Sequence\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has a Sequence record for a TreeGenes record.\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/sequence/hasSequence\",\n" + 
"                \"label\": \"has Sequence\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 1,\n" + 
"                \"max\": 1,\n" + 
"                \"range\": {\n" + 
"                  \"label\": \"string\",\n" + 
"                  \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"                },\n" + 
"                \"description\": \"A unique ID for a TreeGenes Amplicon; e.g., 0_8156_01\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/id\",\n" + 
"                \"label\": \"Amplicon ID\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"min\": 0,\n" + 
"                \"max\": 2147483647,\n" + 
"                \"range\": {\n" + 
"                  \"inputs\": [\n" + 
"                    {\n" + 
"                      \"min\": 0,\n" + 
"                      \"max\": 2147483647,\n" + 
"                      \"range\": {\n" + 
"                        \"inputs\": [\n" + 
"                          {\n" + 
"                            \"label\": \"ForwardPrimer\",\n" + 
"                            \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/ForwardPrimer\"\n" + 
"                          },\n" + 
"                          {\n" + 
"                            \"label\": \"Primer\",\n" + 
"                            \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/Primer\"\n" + 
"                          }\n" + 
"                        ],\n" + 
"                        \"label\": \"\",\n" + 
"                        \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                      },\n" + 
"                      \"description\": \"A property which has a Forward Primer record for a TreeGenes record\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/treeGenes/primer/hasForwardPrimer\",\n" + 
"                      \"label\": \"has Forward Primer\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"min\": 0,\n" + 
"                      \"max\": 2147483647,\n" + 
"                      \"range\": {\n" + 
"                        \"label\": \"EST\",\n" + 
"                        \"type\": \"http://sswapmeet.sswap.info/treeGenes/est/EST\"\n" + 
"                      },\n" + 
"                      \"description\": \"A property which hasEST record for a TreeGenes contig;\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/treeGenes/est/hasEST\",\n" + 
"                      \"label\": \"has EST\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"min\": 0,\n" + 
"                      \"max\": 2147483647,\n" + 
"                      \"range\": {\n" + 
"                        \"inputs\": [\n" + 
"                          {\n" + 
"                            \"label\": \"OBO\",\n" + 
"                            \"type\": \"http://sswapmeet.sswap.info/OBO/OBO\"\n" + 
"                          },\n" + 
"                          {\n" + 
"                            \"label\": \"Term\",\n" + 
"                            \"type\": \"http://sswapmeet.sswap.info/OBO/Term\"\n" + 
"                          },\n" + 
"                          {\n" + 
"                            \"label\": \"GO\",\n" + 
"                            \"type\": \"http://sswapmeet.sswap.info/OBO/GO\"\n" + 
"                          }\n" + 
"                        ],\n" + 
"                        \"label\": \"\",\n" + 
"                        \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                      },\n" + 
"                      \"description\": \"Standardized predicate for any \\\"thing\\\" to assert a relationship  to something of type obo:GO.\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/OBO/hasGO\",\n" + 
"                      \"label\": \"hasGO\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"min\": 1,\n" + 
"                      \"max\": 1,\n" + 
"                      \"range\": {\n" + 
"                        \"label\": \"string\",\n" + 
"                        \"type\": \"http://www.w3.org/2001/XMLSchema#string\"\n" + 
"                      },\n" + 
"                      \"description\": \"A unique ID for a TreeGenes Contig; e.g., 9564\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/treeGenes/contig/id\",\n" + 
"                      \"label\": \"Contig ID\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"min\": 0,\n" + 
"                      \"max\": 2147483647,\n" + 
"                      \"range\": {\n" + 
"                        \"inputs\": [\n" + 
"                          {\n" + 
"                            \"label\": \"ReversePrimer\",\n" + 
"                            \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/ReversePrimer\"\n" + 
"                          },\n" + 
"                          {\n" + 
"                            \"label\": \"Primer\",\n" + 
"                            \"type\": \"http://sswapmeet.sswap.info/treeGenes/primer/Primer\"\n" + 
"                          }\n" + 
"                        ],\n" + 
"                        \"label\": \"\",\n" + 
"                        \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                      },\n" + 
"                      \"description\": \"A property which has a Reverse Primer record for a TreeGenes record\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/treeGenes/primer/hasReversePrimer\",\n" + 
"                      \"label\": \"has Reverse Primer\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"min\": 0,\n" + 
"                      \"max\": 2147483647,\n" + 
"                      \"range\": {\n" + 
"                        \"label\": \"Taxa\",\n" + 
"                        \"type\": \"http://sswapmeet.sswap.info/taxa/Taxa\"\n" + 
"                      },\n" + 
"                      \"description\": \"Standardized predicate for any \\\"thing\\\" to assert a relationship to a Taxa\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/taxa/hasTaxa\",\n" + 
"                      \"label\": \"hasTaxa\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    },\n" + 
"                    {\n" + 
"                      \"min\": 0,\n" + 
"                      \"max\": 2147483647,\n" + 
"                      \"range\": {\n" + 
"                        \"label\": \"Expasy\",\n" + 
"                        \"type\": \"http://sswapmeet.sswap.info/treeGenes/expasy/Expasy\"\n" + 
"                      },\n" + 
"                      \"description\": \"A property which has an ExPASy Enzyme  record for a TreeGenes contig\",\n" + 
"                      \"property\": \"http://sswapmeet.sswap.info/treeGenes/expasy/hasExpasy\",\n" + 
"                      \"label\": \"has ExPASy\",\n" + 
"                      \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"                    }\n" + 
"                  ],\n" + 
"                  \"label\": \"\",\n" + 
"                  \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"                },\n" + 
"                \"description\": \"A property which has a Contig record for a TreeGenes record\",\n" + 
"                \"property\": \"http://sswapmeet.sswap.info/treeGenes/contig/hasContig\",\n" + 
"                \"label\": \"has Contig\",\n" + 
"                \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"              }\n" + 
"            ],\n" + 
"            \"label\": \"\",\n" + 
"            \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"          },\n" + 
"          \"description\": \"A property which has an Amplicon record for a TreeGenes contig\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/treeGenes/amplicon/hasAmplicon\",\n" + 
"          \"label\": \"has Amplicon\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        },\n" + 
"        {\n" + 
"          \"min\": 0,\n" + 
"          \"max\": 2147483647,\n" + 
"          \"range\": {\n" + 
"            \"inputs\": [\n" + 
"              {\n" + 
"                \"label\": \"DataFormat\",\n" + 
"                \"type\": \"http://sswapmeet.sswap.info/data/DataFormat\"\n" + 
"              },\n" + 
"              {\n" + 
"                \"label\": \"Sequence\",\n" + 
"                \"type\": \"http://sswapmeet.sswap.info/sequence/Sequence\"\n" + 
"              }\n" + 
"            ],\n" + 
"            \"label\": \"\",\n" + 
"            \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"          },\n" + 
"          \"description\": \"A predicate allowing any \\\"thing\\\" to assert a relationship to a Sequence object.\",\n" + 
"          \"property\": \"http://sswapmeet.sswap.info/sequence/hasSequence\",\n" + 
"          \"label\": \"has Sequence\",\n" + 
"          \"type\": \"http://www.w3.org/2002/07/owl#Restriction\"\n" + 
"        }\n" + 
"      ],\n" + 
"      \"description\": \"TreeGenes notion of an Alignment.\",\n" + 
"      \"label\": \"Alignment\",\n" + 
"      \"type\": \"http://www.w3.org/2002/07/owl#intersectionOf\"\n" + 
"    }\n" + 
"  ],\n" + 
"  \"description\": \"Accepts a TreeGenes Tree Sample Id, Contig Record, Amplicon Record, Alignment Record, or TreeSample Record.\",\n" + 
"  \"valueIndex\": 3,\n" + 
"  \"label\": \"Tree Sample Service Request\",\n" + 
"  \"valueTypes\": [\n" + 
"    \"http://sswapmeet.sswap.info/treeGenes/treesample/TreeSample\",\n" + 
"    \"http://sswapmeet.sswap.info/treeGenes/contig/Contig\",\n" + 
"    \"http://sswapmeet.sswap.info/treeGenes/amplicon/Amplicon\",\n" + 
"    \"http://sswapmeet.sswap.info/treeGenes/alignment/Alignment\"\n" + 
"  ],\n" + 
"  \"type\": \"http://www.w3.org/2002/07/owl#unionOf\"\n" + 
"}";
	
	public static void main(String[] args) throws Exception {
		// some types to test
		// Type:
		// http://sswap-c.iplantcollaborative.org/test/ontologies/qtl/QtlByTraitAccessionRequest
		// Required: trait:accessionID, trait:name, trait:symbol Optional:
		// trait:synonym
		// process("http://sswap-c.iplantcollaborative.org/test/ontologies/qtl/QtlByTraitAccessionRequest");
		// process("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/AnnotationSearchRequest");
//		processRDG("http://sswap.info/iplant/svc/quicktree-tree-lonestar-1.1");
//		processRDG("file:/Users/evren/sswap/ds/iplantsvc/resources/quicktree-lonestar-1.1/quicktree-tree-lonestar-1.1");
//		process("http://sswapmeet.sswap.info/treeGenes/requests/TreeSampleServiceRequest");
		// process("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/AnnotationDetailsRequest");
		// process("file:AnnotationDetailsRequest.owl",
		// "http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/AnnotationDetailsRequest");
		// process("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/AnnotatedPOTerm");
		// process("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/Annotation");
		// process("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/AssociationMetadata");
		// process("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/DBxRecord");

		// //
		// http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/TermSearchRequest
		// process("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/TermSearchRequest");

		// process(Namespaces.SSWAP_NS + "Subject");
		
//		Input input = process("http://sswapmeet.sswap.info/iplant/quicktree-lonestar-1.1/requests/QuickTreeRequest");
//		((UnionInput)input).setValueIndex(0);
		
		processRDG("http://sswap.info/iplant/svc/quicktree-dm-lonestar-1.1");
		
//		SSWAPDocument rrg = SSWAP.getResourceGraph(new ByteArrayInputStream(rdf.getBytes()), RRG.class);
//		SSWAPObject object = rrg.getResource().getGraph().getSubject().getObject();		
//		SSWAPIndividualDeserializer deserializer = new SSWAPIndividualDeserializer();
//		
//		Input input = deserializer.deserialize(object); 
//		System.out.println("Deserialized input for " + object.getID() + ":\n"+Inputs.toJSONString(input));

//		Input input = Inputs.fromJSON(new JSONObject(rdf));
//		Model model = ModelFactory.createDefaultModel();
//		new JenaSerializer().serialize(input, model);
//		model.write(System.out, "TTL", "TTL");
//		System.out.println("Deserialized input for " + object.getID() + ":\n"+Inputs.toJSONString(input));
		

	}

	public static Input processRDG(String serviceURI) {
		RDG rdg = SSWAP.getRDG(URI.create(serviceURI));
		SSWAPResource resource = rdg.getResource();

		Input input = new SSWAPIndividualDeserializer().deserialize(resource);
		System.out.println(input);
		System.out.println();
		System.out.println(Inputs.toPrettyString(input));
		System.out.println();
		return input;
	}

	public static Input process(String uri) throws Exception {
		return process(uri, uri);
	}

	public static Input process(String sourceURI, String typeURI)
			throws Exception {
		URI uri = URI.create(typeURI);
		SSWAPDocument doc = SSWAP.getResourceGraph(URI.create(sourceURI)
				.toURL().openStream(), SSWAPDocument.class, uri);

		SSWAPType type = doc.getType(uri);
		doc.getReasoningService();

		System.out.println();
		System.out.println("Type: " + uri);
		Input input = Inputs.fromSSWAP(type);
		System.out.println(input);
		System.out.println();
		System.out.println(Inputs.toPrettyString(input));
		System.out.println();

		JSONObject json = Inputs.toJSON(input);
		System.out.println(json.toString(2));
		System.out.println();
		System.out.println(Inputs.toPrettyString(Inputs.fromJSON(json)));
		System.out.println();
		return input;
	}

}
