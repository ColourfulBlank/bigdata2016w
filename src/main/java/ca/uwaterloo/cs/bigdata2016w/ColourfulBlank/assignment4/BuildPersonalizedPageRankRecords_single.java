// package ca.uwaterloo.cs.bigdata2016w.ColourfulBlank.assignment4;

// import java.io.IOException;
// import java.util.Arrays;
// import java.util.ArrayList;

// import org.apache.commons.cli.CommandLine;
// import org.apache.commons.cli.CommandLineParser;
// import org.apache.commons.cli.GnuParser;
// import org.apache.commons.cli.HelpFormatter;
// import org.apache.commons.cli.OptionBuilder;
// import org.apache.commons.cli.Options;
// import org.apache.commons.cli.ParseException;
// import org.apache.hadoop.conf.Configuration;
// import org.apache.hadoop.conf.Configured;
// import org.apache.hadoop.fs.FileSystem;
// import org.apache.hadoop.fs.Path;
// import org.apache.hadoop.io.IntWritable;
// import org.apache.hadoop.io.LongWritable;
// import org.apache.hadoop.io.Text;
// import org.apache.hadoop.mapreduce.Job;
// import org.apache.hadoop.mapreduce.Mapper;
// import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
// import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
// import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
// import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
// import org.apache.hadoop.util.Tool;
// import org.apache.hadoop.util.ToolRunner;
// import org.apache.log4j.Logger;

// import tl.lin.data.array.ArrayListOfIntsWritable;

// /**
//  * <p>
//  * Driver program that takes a plain-text encoding of a directed graph and builds corresponding
//  * Hadoop structures for representing the graph.
//  * </p>
//  *
//  * @author Jimmy Lin
//  * @author Michael Schatz
//  */
// public class BuildPersonalizedPageRankRecords extends Configured implements Tool {
//   private static final Logger LOG = Logger.getLogger(BuildPersonalizedPageRankRecords.class);

//   private static final String NODE_CNT_FIELD = "node.cnt";

//   private static class MyMapper extends Mapper<LongWritable, Text, IntWritable, PageRankNode> {
//     private static final IntWritable nid = new IntWritable();
//     private static final PageRankNode  = new PageRankNode();
//     private int [] sources;

//     @Override
//     public void setup(Mapper<LongWritable, Text, IntWritable, PageRankNode>.Context context) {
//       int n = context.getConfiguration().getInt(NODE_CNT_FIELD, 0);
//       // System.out.println("NODE_CNT_FIELD: " + n);
//       // System.out.println("-log(NODE_CNT_FIELD): " + (float) -StrictMath.log(n));
//       if (n == 0) {
//         throw new RuntimeException(NODE_CNT_FIELD + " cannot be 0!");
//       }
//       int sourceLength = context.getConfiguration().getInt("NumberOfSources", 0);
//       sources = new int[sourceLength];
//       for (int i = 0; i < sourceLength; i++){
//         sources[i] = context.getConfiguration().getInt("source"+i, 0);
//       }
//       node.setType(PageRankNode.Type.Complete);
//       // node.setPageRank((float) -StrictMath.log(n));
//     }

//     @Override
//     public void map(LongWritable key, Text t, Context context) throws IOException,
//         InterruptedException {
//       String[] arr = t.toString().trim().split("\\s+");
      
//       nid.set(Integer.parseInt(arr[0]));
//       if (arr.length == 1) {
//           node.setNodeId(Integer.parseInt(arr[0]));
//           node.setAdjacencyList(new ArrayListOfIntsWritable());

//       } else {//set neighbors
//         // node.setNodeId(Integer.parseInt(arr[0]));
//           node.setNodeId(Integer.parseInt(arr[0]), itera);
          
//           int[] neighbors = new int[arr.length - 1];
          
//           for (int i = 1; i < arr.length; i++) {
//             neighbors[i - 1] = Integer.parseInt(arr[i]);
//           }

//           node.setAdjacencyList(new ArrayListOfIntsWritable(neighbors));
//       }

//       context.getCounter("graph", "numNodes").increment(1);
//       context.getCounter("graph", "numEdges").increment(arr.length - 1);

//       if (arr.length > 1) {
//         context.getCounter("graph", "numActiveNodes").increment(1);
//       }
//       if (nid.get() == sources[0]){// need to change to become multi
//         // node.setPageRank(1.0f);
//           node.setPageRank(1.0f);

//       }else {
//           node.setPageRank(0.0f);
//       }
//       context.write(nid, node);
//     }
//   }

//   public BuildPersonalizedPageRankRecords() {}

//   private static final String INPUT = "input";
//   private static final String OUTPUT = "output";
//   private static final String NUM_NODES = "numNodes";
//   private static final String SOURCES = "sources";

//   /**
//    * Runs this tool.
//    */
//   @SuppressWarnings({ "static-access" })
//   public int run(String[] args) throws Exception {
//     Options options = new Options();

//     options.addOption(OptionBuilder.withArgName("path").hasArg()
//         .withDescription("input path").create(INPUT));
//     options.addOption(OptionBuilder.withArgName("path").hasArg()
//         .withDescription("output path").create(OUTPUT));
//     options.addOption(OptionBuilder.withArgName("num").hasArg()
//         .withDescription("number of nodes").create(NUM_NODES));
//     options.addOption(OptionBuilder.withArgName("num").hasArg()
//         .withDescription("sources").create(SOURCES));

//     CommandLine cmdline;
//     CommandLineParser parser = new GnuParser();

//     try {
//       cmdline = parser.parse(options, args);
//     } catch (ParseException exp) {
//       System.err.println("Error parsing command line: " + exp.getMessage());
//       return -1;
//     }

//     if (!cmdline.hasOption(INPUT) || !cmdline.hasOption(OUTPUT) || !cmdline.hasOption(NUM_NODES)) {
//       System.out.println("args: " + Arrays.toString(args));
//       HelpFormatter formatter = new HelpFormatter();
//       formatter.setWidth(120);
//       formatter.printHelp(this.getClass().getName(), options);
//       ToolRunner.printGenericCommandUsage(System.out);
//       return -1;
//     }

//     String inputPath = cmdline.getOptionValue(INPUT);
//     String outputPath = cmdline.getOptionValue(OUTPUT);
//     int n = Integer.parseInt(cmdline.getOptionValue(NUM_NODES));
//     String [] sourcesString = cmdline.getOptionValue(SOURCES).split(",");
//     int [] sources = new int[sourcesString.length];
//     for (int i = 0; i < sources.length; i++){
//       sources[i] = Integer.parseInt(sourcesString[i]);
//     }

//     LOG.info("Tool name: " + BuildPersonalizedPageRankRecords.class.getSimpleName());
//     LOG.info(" - inputDir: " + inputPath);
//     LOG.info(" - outputDir: " + outputPath);
//     LOG.info(" - numNodes: " + n);
//     LOG.info(" - sourceNum: " + sources.length);
//     for (int i = 0; i < sources.length; i++){
//       LOG.info(" - source"+i+": " + sources[i]);
//     }

//     Configuration conf = getConf();
//     conf.setInt(NODE_CNT_FIELD, n);
//     //setInt sources
//     // conf.setInt(SOURCES, s);
//     conf.setInt("mapred.min.split.size", 1024 * 1024 * 1024);
//     conf.setInt("NumberOfSources", sources.length);
//     for (int i = 0; i < sources.length; i++){
//       conf.setInt("source"+i, sources[i]);
//     }

//     Job job = Job.getInstance(conf);
//     job.setJobName(BuildPersonalizedPageRankRecords.class.getSimpleName() + ":" + inputPath);
//     job.setJarByClass(BuildPersonalizedPageRankRecords.class);

//     job.setNumReduceTasks(0);

//     FileInputFormat.addInputPath(job, new Path(inputPath));
//     FileOutputFormat.setOutputPath(job, new Path(outputPath));

//     job.setInputFormatClass(TextInputFormat.class);
//     job.setOutputFormatClass(SequenceFileOutputFormat.class);

//     job.setMapOutputKeyClass(IntWritable.class);
//     job.setMapOutputValueClass(PageRankNode.class);

//     job.setOutputKeyClass(IntWritable.class);
//     job.setOutputValueClass(PageRankNode.class);

//     job.setMapperClass(MyMapper.class);

//     // Delete the output directory if it exists already.
//     FileSystem.get(conf).delete(new Path(outputPath), true);

//     job.waitForCompletion(true);

//     return 0;
//   }

//   /**
//    * Dispatches command-line arguments to the tool via the {@code ToolRunner}.
//    */
//   public static void main(String[] args) throws Exception {
//     ToolRunner.run(new BuildPersonalizedPageRankRecords(), args);
//   }
// }
