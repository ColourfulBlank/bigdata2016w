package ca.uwaterloo.cs.bigdata2016w.ColourfulBlank.assignment7;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;

import tl.lin.data.array.ArrayListWritable;
import tl.lin.data.fd.Object2IntFrequencyDistribution;
import tl.lin.data.fd.Object2IntFrequencyDistributionEntry;
import tl.lin.data.pair.PairOfInts;
import tl.lin.data.pair.PairOfObjectInt;
import tl.lin.data.pair.PairOfWritables;

public class BuildInvertedIndexHBase extends Configured implements Tool {
  private static final Logger LOG = Logger.getLogger(BuildInvertedIndexHBase.class);

  private static class MyMapper extends Mapper<LongWritable, Text, Text, PairOfInts> {
    private static final Text WORD = new Text();
    private static final Object2IntFrequencyDistribution<String> COUNTS =
        new Object2IntFrequencyDistributionEntry<String>();

    @Override
    public void map(LongWritable docno, Text doc, Context context)
        throws IOException, InterruptedException {
      String text = doc.toString();

      // Tokenize line.
      List<String> tokens = new ArrayList<String>();
      StringTokenizer itr = new StringTokenizer(text);
      while (itr.hasMoreTokens()) {
        String w = itr.nextToken().toLowerCase().replaceAll("(^[^a-z]+|[^a-z]+$)", "");
        if (w.length() == 0) continue;
        tokens.add(w);
      }

      // Build a histogram of the terms.
      COUNTS.clear();
      for (String token : tokens) {
        COUNTS.increment(token);
      }

      // Emit postings.
      for (PairOfObjectInt<String> e : COUNTS) {
        WORD.set(e.getLeftElement());
        context.write(WORD, new PairOfInts((int) docno.get(), e.getRightElement()));
      }
    }
  }

  //HBase stuffs
  public static final String[] FAMILIES = { "p" }; // column family
  public static final byte[] CF = FAMILIES[0].getBytes(); // column family

  public static class MyTableReducer extends TableReducer<Text, PairOfInts, ImmutableBytesWritable>  {


    private final static IntWritable DF = new IntWritable();

    @Override
    public void reduce(Text key, Iterable<PairOfInts> values, Context context)
        throws IOException, InterruptedException {
      Iterator<PairOfInts> iter = values.iterator();
      PairOfInts pair;
      Put put = new Put(Bytes.toBytes(key.toString()));
      while (iter.hasNext()) {
        pair = iter.next().clone();
        put.add(CF, Bytes.toBytes(pair.getLeftElement()), Bytes.toBytes(pair.getRightElement()));
      }

      context.write(null, put);
    }

  }


  private BuildInvertedIndexHBase() {}

  public static class Args {
    @Option(name = "-input", metaVar = "[path]", required = true, usage = "input path")
    public String input;

    @Option(name = "-table", metaVar = "[name]", required = true, usage = "HBase table to store output")
    public String table;

    @Option(name = "-config", metaVar = "[path]", required = true, usage = "HBase config")
    public String config;

    @Option(name = "-reducers", metaVar = "[num]", required = false, usage = "number of reducers")
    public int numReducers = 1;
  }

  /**
   * Runs this tool.
   */
  public int run(String[] argv) throws Exception {
    Args args = new Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(100));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      return -1;
    }

    LOG.info("Tool: " + BuildInvertedIndexHBase.class.getSimpleName());
    LOG.info(" - input path: " + args.input);
    LOG.info(" - output table: " + args.table);
    LOG.info(" - config: " + args.config);
    LOG.info(" - number of reducers: " + args.numReducers);

    // If the table doesn't already exist, create it.
    Configuration conf = getConf();
    conf.addResource(new Path(args.config));

    Configuration hbaseConfig = HBaseConfiguration.create(conf);
    HBaseAdmin admin = new HBaseAdmin(hbaseConfig);

    if (admin.tableExists(args.table)) {
      LOG.info(String.format("Table '%s' exists: dropping table and recreating.", args.table));
      LOG.info(String.format("Disabling table '%s'", args.table));
      admin.disableTable(args.table);
      LOG.info(String.format("Droppping table '%s'", args.table));
      admin.deleteTable(args.table);
    }

    HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(args.table));
    for (int i = 0; i < FAMILIES.length; i++) {
      HColumnDescriptor hColumnDesc = new HColumnDescriptor(FAMILIES[i]);
      tableDesc.addFamily(hColumnDesc);
    }
    admin.createTable(tableDesc);
    LOG.info(String.format("Successfully created table '%s'", args.table));

    admin.close();

    Job job = Job.getInstance(getConf());
    job.setJobName(BuildInvertedIndexHBase.class.getSimpleName());
    job.setJarByClass(BuildInvertedIndexHBase.class);

    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(PairOfInts.class);

    job.setMapperClass(MyMapper.class);
    job.setNumReduceTasks(args.numReducers);//!

    FileInputFormat.setInputPaths(job, new Path(args.input));
    TableMapReduceUtil.initTableReducerJob(args.table, MyTableReducer.class, job);

    long startTime = System.currentTimeMillis();
    job.waitForCompletion(true);
    System.out.println("Job Finished in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");

    return 0;
  }

  /**
   * Dispatches command-line arguments to the tool via the {@code ToolRunner}.
   */
  public static void main(String[] args) throws Exception {
    ToolRunner.run(new BuildInvertedIndexHBase(), args);
  }
}
