#!/bin/sh

java -jar target/c3pro-data-0.1.0-SNAPSHOT-standalone.jar ChangePropagationLog.csv ChangePropagationLogFinal.csv ChangePropagationStats.csv ChangePropagationStatsFinal.csv ChangePropagationStatsFinalWithoutNoise.csv
#rm -f *.pdf && R CMD BATCH "plot_c3pro.r" && open test.pdf
#rm -f *.pdf && R CMD BATCH "plot_c3pro.r"
R CMD BATCH "plot_c3pro.r"
