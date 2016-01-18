#!/bin/bash

mvn clean package

hadoop jar target/bigdata2016w-0.1.0-SNAPSHOT.jar    ca.uwaterloo.cs.bigdata2016w.ColourfulBlank.assignment1.PairsPMI    -input data/Shakespeare.txt -output cs489-2016w-lintool-a1-shakespeare-pairs -reducers 5

