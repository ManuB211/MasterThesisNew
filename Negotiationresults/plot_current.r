library("ggplot2")
library("reshape2")
library("gridExtra")

theme_set(theme_bw())

colors <- c("#BF4717", "#608FE2", "#F4E005")

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

data <- read.table(file="NegotiationLog4.csv", header=T, sep=";")
data["Fairness"] = abs(data["Utility_Bank"] - data["Utility_Insurance"])
data["Efficiency"] = (data["Utility_Bank"] + data["Utility_Insurance"]) / 2

additiveData = data[ which(data$Type == "additiveScore"), ]
nashData = data[ which(data$Type == "nashbargainScore"), ]
rksData = data[ which(data$Type == "rksScore"), ]

chart1 <- ggplot() + labs(title="Additive Solution") + geom_point(data=additiveData, aes(x=Utility_Bank, y=Utility_Insurance, fill=Fairness), pch=23, size=3)
chart2 <- ggplot() + labs(title="Nash Bargaining Solution") + geom_point(data=nashData, aes(x=Utility_Bank, y=Utility_Insurance, fill=Fairness), pch=24, size=3)
chart3 <- ggplot() + labs(title="RKS Solution") + geom_point(data=rksData, aes(x=Utility_Bank, y=Utility_Insurance, fill=Fairness), pch=21, size=3)
#chart4 <- ggplot() + geom_point(data=data, aes(x=Utility_Bank, y=Utility_Insurance, fill=factor(Type),colour=factor(Type), group=data[,Type]), pch=21, size=3)

#data["Utility_Bank"] = round(data["Utility_Bank"] * 10000)
#data["Utility_Insurance"] = round(data["Utility_Insurance"] * 10000)

#chart1 <- plot_points_chart(data, "Utility_Bank", "Utility_Insurance", "AdditiveScore",
                            #"Utility Scatterplot (Additive)",
                            #"Utility(Bank)",
                            #"Utility(Insurance)",
                            #"legend",
                            #colors)

pdf(file="test.pdf")
grid.arrange(chart1, chart2, chart3, ncol=2)
dev.off()
