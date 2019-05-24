From busybox:glibc

RUN mkdir -p /root/workplace
COPY ./app1 /root/workplace/app1
RUN chmod +x /root/workplace/app1

WORKDIR /root/workplace
ENTRYPOINT ["/root/workplace/app1"]
