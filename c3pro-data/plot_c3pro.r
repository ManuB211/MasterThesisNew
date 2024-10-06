library("ggplot2")
library("reshape2")
library("gridExtra")

theme_set(theme_bw())

data <- read.table(file="ChangePropagationStatsFinal.csv", header=T, sep=",")
data_no_noise <- read.table(file="ChangePropagationStatsFinalWithoutNoise.csv", header=T, sep=",")
log_data <- read.csv("ChangePropagationLogFinal.csv")

# delete, insert, replace
#colors <- c("#3799DE", "#4A585E", "#BAB7A8")
colors <- c("#BF4717", "#608FE2", "#F4E005")

plot_grouped_line_chart <- function(data_src, xcol, ycol, grouped_by, title, xlabel, ylabel, legend_title, colors) {
    data_src$xcol <- data_src[,xcol]
    data_src$ycol <- data_src[,ycol]
    data_src$grouped_by <- data_src[,grouped_by]

    chart <- ggplot(data_src, aes(x=xcol, y=ycol, colour=grouped_by, group=grouped_by)) +
        theme(legend.position="top") +
        geom_line() +
        geom_point(size=2, shape=21, fill="white") +
        xlab(xlabel) +
        ylab(ylabel) +
        labs(title=title) +
        scale_colour_manual(name=legend_title, values=colors) # we have to scale to set legend title. we set our custom colors also

    return(chart)
}

plot_points_chart <- function(data_src, xcol, ycol, grouped_by, title, xlabel, ylabel, legend_title, colors) {
    data_src$xcol <- data_src[,xcol]
    data_src$ycol <- data_src[,ycol]
    data_src$grouped_by <- data_src[,grouped_by]

    chart <- ggplot(data_src, aes(x=xcol, y=ycol, colour=grouped_by, group=grouped_by)) +
        labs(title=title) +
        theme(legend.position="top") +
        #geom_line() +
        geom_point(size=2, shape=3, fill="white") +
        #geom_smooth(method=lm) +
        xlab(xlabel) +
        ylab(ylabel) +
        #scale_colour_hue(name="", l=40) +
        scale_colour_manual(name=legend_title, values = colors)
    return (chart)
}

plot_filtered_line_chart <- function(data_src, type, xcol, ycol, title, xlabel, ylabel, color) {
    fdata <- data_src[ which(data_src$Type == type), ]
    fdata$xcol <- fdata[,xcol]
    fdata$ycol <- fdata[,ycol]
    chart <- ggplot(fdata, aes(x=xcol, y=ycol)) +
        labs(title=title) +
        geom_line(colour=color) +
        geom_point(size=2, shape=21, fill="white", colour=color) +
        xlab(xlabel) +
        ylab(ylabel)
        #scale_colour_hue(name="", l=40)
        #scale_colour_manual(values = colors)
    #ggsave("insert_cp_affected.pdf")
    return(chart)
}

plot_filtered_and_grouped_line_chart <- function(data_src, type, xcol, ycol, grouped_by, title, xlabel, ylabel, legend_title, colors) {
    fdata <- data_src[ which(data_src$Type == type), ]
    fdata$xcol <- fdata[,xcol]
    fdata$ycol <- fdata[,ycol]
    fdata$grouped_by <- fdata[,grouped_by]

    chart <- ggplot(fdata, aes(x=xcol, y=ycol, colour=grouped_by, group=grouped_by)) +
        theme(legend.position="top") +
        geom_line() +
        geom_point(size=2, shape=21, fill="white") +
        xlab(xlabel) +
        ylab(ylabel) +
        labs(title=title)
        #scale_colour_manual(name=legend_title, values=colors) # we have to scale to set legend title. we set our custom colors also
        #scale_colour_manual(name=legend_title)

    return(chart)
}

plot_filtered_bar_chart <- function(data_src, type, xcol, ycol, title, xlabel, ylabel) {
    # TODO: maybe filter by type? or take average value because the bars show many points
    fdata <- data_src[ which(data_src$Type == type), ]
    fdata$xcol <- fdata[,xcol]
    fdata$ycol <- fdata[,ycol]

    unique_data = unique(fdata[c("xcol", "ycol")])

    chart <- ggplot(unique_data, aes(x=xcol, y=ycol, fill=xcol)) +
        geom_bar(colour="black", stat="identity") +
        #facet_wrap(~ xcol)
        guides(fill=FALSE) +
        xlab(xlabel) +
        ylab(ylabel) +
        labs(title=title)
    #chart <- qplot(xcol, data=unique_data, geom="bar", weight=ycol, ylab=ylabel)
    write.csv(unique_data, file = paste("data_", type, ".csv", sep=""))

    return(chart)
}

plot_facet_bar_chart <- function(data_src, facet_col, xcol, ycol, title, xlabel, ylabel) {
    fdata <- data_src
    fdata$facet_col <- data_src[,facet_col]
    fdata$xcol      <- data_src[,xcol]
    fdata$ycol      <- data_src[,ycol]

    unique_data = unique(fdata[c("facet_col", "xcol", "ycol")])
    chart <- ggplot(unique_data, aes(x=xcol, ycol, fill=xcol)) +
        geom_bar(colour="black", stat="identity") +
        facet_wrap(~ facet_col) +
        guides(fill=FALSE) +
        xlab(xlabel) +
        ylab(ylabel) +
        labs(title=title)
    write.csv(unique_data, file = "data_all.csv")
    return(chart)
}

plot_all_for_data <- function(data_src, filename_prefix) {

    chart1 <- plot_grouped_line_chart(data_src, "Nb_Nodes_source", "avg_nodes_target_by_type_size",
                              "Type",
                              "(a) # of affected Nodes during Change Propagation",
                              "Source Nodes Count",
                              "Avg. Target Nodes Affected",
                              "Operation Type",
                              colors)

    chart2 <- plot_grouped_line_chart(data_src, "Nb_Nodes_source", "avg_nodes_target",
                              "structure_type",
                              "(b) # of affected Nodes categorized by structural type",
                              "Source Nodes Count",
                              "Avg. Target Nodes Affected",
                              "Structural Type",
                              colors)

    pdf(file=paste(filename_prefix, "_source_vs_target_nodes.pdf", sep=""))
    grid.arrange(chart1, chart2, ncol=1)
    dev.off()

    # Partition Change Types by partner and draw plots
    chart_insert_grouped_by_partners <- plot_filtered_bar_chart(data_src, "Insert", "short_partner_name", "impact_ratio_per_partner",
        "Impact Ratio by Role (Insert)",
        "Partner",
        "Impact Ratio")

    chart_delete_grouped_by_partners <- plot_filtered_bar_chart(data_src, "Delete", "short_partner_name", "impact_ratio_per_partner",
        "Impact Ratio by Role (Delete)",
        "Partner",
        "Impact Ratio")

    chart_replace_grouped_by_partners <- plot_filtered_bar_chart(data_src, "Replace", "short_partner_name", "impact_ratio_per_partner",
        "Impact Ratio by Role (Replace)",
        "Partner",
        "Impact Ratio")

    pdf(file=paste(filename_prefix, "_impact_ratio_by_partner_insert.pdf", sep=""))
    grid.arrange(chart_insert_grouped_by_partners, ncol=1)
    dev.off()

    pdf(file=paste(filename_prefix, "_impact_ratio_by_partner_delete.pdf", sep=""))
    grid.arrange(chart_delete_grouped_by_partners, ncol=1)
    dev.off()

    pdf(file=paste(filename_prefix, "_impact_ratio_by_partner_replace.pdf", sep=""))
    grid.arrange(chart_replace_grouped_by_partners, ncol=1)
    dev.off()

    pdf(file=paste(filename_prefix, "_impact_ratio_by_partner_combined.pdf", sep=""))
    grid.arrange(chart_insert_grouped_by_partners,
                 chart_delete_grouped_by_partners,
                 chart_replace_grouped_by_partners,
                 ncol=1)
    dev.off()

    chart_all_grouped_by_partners <- plot_facet_bar_chart(data_src, "Partner", "Type", "impact_ratio_per_partner",
        "Impact Ratio by Role",
        "Type",
        "Impact Ratio")

    pdf(file=paste(filename_prefix, "_impact_ratio_by_partner_all.pdf", sep=""))
    grid.arrange(chart_all_grouped_by_partners, ncol=1)
    dev.off()

    #
    # Create Source vs Target chart for each operation type
    #

    chart3 <- plot_filtered_line_chart(data_src, "Insert", "Nb_Nodes_source", "avg_nodes_target_by_type_size",
        "# of affected nodes during Changed Propagation (Insert)",
        "Source Nodes Count",
        "Avg. Target Nodes Affected",
        colors[2])

    chart4 <- plot_filtered_line_chart(data_src, "Delete", "Nb_Nodes_source", "avg_nodes_target_by_type_size",
        "# of affected nodes during Change Propagation (Delete)",
        "Source Nodes Count",
        "Avg. Target Nodes Affected",
        colors[1])

    chart5 <- plot_filtered_line_chart(data_src, "Replace", "Nb_Nodes_source", "avg_nodes_target_by_type_size",
        "# of affected nodes during Change Propagation (Replace)",
        "Source Nodes Count",
        "Avg. Target Nodes Affected",
        colors[3])

    pdf(file=paste(filename_prefix, "_individual_source_vs_target_nodes.pdf", sep=""))
    grid.arrange(chart3, chart4, chart5, ncol=1)
    dev.off()

    chart6 <- plot_points_chart(data_src, "Nb_Nodes_source", "exec_time",
        "Type",
        "Execution Time by Change Operation Type",
        "Source Nodes Count",
        "Execution Time (ms)",
        "Operation Type",
        colors)

    chart7 <- plot_grouped_line_chart(data_src, "Nb_Nodes_source", "avg_exec_time_by_type",
        "Type",
        "Avg. Execution Time by Change Operation Type",
        "Source Nodes Count",
        "Avg. Execution Time (ms)",
        "Operation Type",
        colors)

    pdf(file=paste(filename_prefix, "_source_vs_execution_time.pdf", sep=""))
    grid.arrange(chart6, chart7, ncol=1)
    dev.off()

    #
    # Create Source vs Execution chart for each operation type
    #

    chart_src_vs_exec1 <- plot_filtered_line_chart(data_src, "Insert", "Nb_Nodes_source", "avg_exec_time_by_type",
        "Avg. Execution Time by Change Operation Type (Insert)",
        "Source Nodes Count",
        "Avg. Execution Time (ms)",
        colors[2])

    chart_src_vs_exec2 <- plot_filtered_line_chart(data_src, "Delete", "Nb_Nodes_source", "avg_exec_time_by_type",
        "Avg. Execution Time by Change Operation Type (Delete)",
        "Source Nodes Count",
        "Avg. Execution Time (ms)",
        colors[1])

    chart_src_vs_exec3 <- plot_filtered_line_chart(data_src, "Replace", "Nb_Nodes_source", "avg_exec_time_by_type",
        "Avg. Execution Time by Change Operation Type (Replace)",
        "Source Nodes Count",
        "Avg. Execution Time (ms)",
        colors[3])

    pdf(file=paste(filename_prefix, "_individual_source_vs_execution.pdf", sep=""))
    grid.arrange(chart_src_vs_exec1, chart_src_vs_exec2, chart_src_vs_exec3, ncol=1)
    dev.off()

    #
    # Point Plot Chart for source vs target
    #

    source_vs_target_point_plot_chart <- plot_points_chart(data_src, "Nb_Nodes_source", "Nb_Nodes_target",
        "Type",
        "# of affected Target Nodes by Change Operation Type",
        "Source Nodes Count",
        "# of affected target nodes",
        "Operation Type",
        colors)

    pdf(file=paste(filename_prefix, "_source_vs_target_point_plot.pdf", sep=""))
    grid.arrange(source_vs_target_point_plot_chart, ncol=1)
    dev.off()
}

plot_all_for_data(data, "_default")
plot_all_for_data(data_no_noise, "_no_noise")

# calculate magnitude
chart <- ggplot(data, aes(x=magnitude)) +
        geom_bar(colour="black", stat="bin") +
        #facet_wrap(~ xcol)
        guides(fill=FALSE) +
        xlab("Magnitude") +
        ylab("count") +
        labs(title="Magnitude histogram")

pdf(file=paste("_magnitude.pdf", sep=""))
grid.arrange(chart, ncol=1)
dev.off()

calculate_cpi <- function(log_data) {
    # calculate CPI
    log_data_x <- unique(log_data[c("ChgRequestor", "out_degree")])
    log_data_y <- unique(log_data[c("AffectedPartner", "in_degree")])
    colnames(log_data_x) <- c("partner", "out_degree")
    colnames(log_data_y) <- c("partner", "in_degree")
    log_data_final <- merge(log_data_x, log_data_y, by="partner")
    # create new CPI column
    log_data_final["CPI"] <- NA
    log_data_final$CPI <- (log_data_final$out_degree - log_data_final$in_degree) / (log_data_final$out_degree + log_data_final$in_degree)
    x <- log_data_final[order(log_data_final$CPI),]
    x$type[x$CPI >= 0.3] = "1) strong multipliers\n    (CPI >= 0.3)\n"
    x$type[x$CPI >= 0.1 & x$CPI < 0.3] = "2) weak multipliers\n    (0.1 <= CPI < 0.3)\n"
    x$type[x$CPI >= -0.1 & x$CPI < 0.1] = "3) carriers\n    (-0.1 <= CPI < 0.1)\n"
    x$type[x$CPI >= -0.3 & x$CPI < -0.1] = "4) weak absorbers\n    (-0.3 <= CPI < -0.1)\n"
    x$type[x$CPI <= -0.3] = "5) strong absorbers\n    (CPI <= -0.3)\n"
    x$type <- factor(x$type)
    chart <- dotchart(x$CPI, labels=x$partner, cex=.7, groups=x$type, main="Partner-CPI", xlab="CPI")
    return(chart)
}

# the grid.arrange way does not work with dotchart
pdf(file=paste("_partner_cpi.pdf", sep=""))
print(calculate_cpi(log_data))
dev.off()
