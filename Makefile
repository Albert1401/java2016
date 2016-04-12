pr_libs=java-advanced-2016/lib
pr_artifacts=java-advanced-2016/artifacts
compiled=out/production/java2016
pr_mypkg=ru.ifmo.ctddev.gafarov
pr_kgpkg=info.kgeorgiy.java.advanced

CPDEFAULT="-cp ${pr_libs}/*:${compiled}:${pr_artifacts}/"

build:
	find src/ -name *.java | xargs javac -d out/production/java2016/ -cp "java-advanced-2016/artifacts/*:java-advanced-2016/lib/*"

pull:
	git -C java-advanced-2016/ pull

hw2:
	java "${CPDEFAULT}/ArraySetTest.jar" ${pr_kgpkg}.arrayset.Tester NavigableSet ${pr_mypkg}.arrayset.ArraySet

hw3:
	java "${CPDEFAULT}/ImplementorTest.jar" ${pr_kgpkg}.implementor.Tester class ${pr_mypkg}.implementor.Implementor

hw4:

hw5:
	java "${CPDEFAULT}/ImplementorTest.jar" ${pr_kgpkg}.implementor.Tester jar-class ${pr_mypkg}.implementor.Implementor

hw6:
	java "${CPDEFAULT}/IterativeParallelismTest.jar" ${pr_kgpkg}.concurrent.Tester list ${pr_mypkg}.concurrent.IterativeParallelism
hw7:
	java "${CPDEFAULT}/ParallelMapperTest.jar" ${pr_kgpkg}.mapper.Tester list ${pr_mypkg}.mapper.ParallelMapperImpl,${pr_mypkg}.mapper.IterativeParallelism
hw8:
	java "${CPDEFAULT}/WebCrawlerTest.jar" ${pr_kgpkg}.crawler.Tester easy ${pr_mypkg}.crawler.WebCrawler
