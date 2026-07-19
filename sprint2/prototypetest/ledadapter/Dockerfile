FROM eclipse-temurin:17.0.5_8-jre-focal
ADD ./build/distributions/ledadapter-1.0.tar /
WORKDIR /ledadapter-1.0/bin
COPY ./*.pl ./
CMD ["bash", "ledadapter"]
