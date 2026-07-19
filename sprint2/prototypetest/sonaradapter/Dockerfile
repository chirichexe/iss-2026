FROM eclipse-temurin:17.0.5_8-jre-focal
ADD ./build/distributions/sonaradapter-1.0.tar /
WORKDIR /sonaradapter-1.0/bin
COPY ./*.pl ./
CMD ["bash", "sonaradapter"]
