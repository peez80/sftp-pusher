FROM openjdk:8
COPY ./ ./
RUN ./gradlew installDist


FROM openjdk:8
COPY --from=0 ./build/install/foldersync/ /opt/foldersync/
ENTRYPOINT ["/opt/foldersync/bin/foldersync"]
