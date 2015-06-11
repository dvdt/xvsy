from: https://help.ubuntu.com/community/PostgreSQL

sudo -u postgres psql postgres
\password postgres
 sudo -u postgres createdb airlines

# allow WAN access
host all all 0.0.0.0/0 md5

listen_addresses = '*'

data: http://stat-computing.org/dataexpo/2009/2008.csv.bz2

cd /var/lib/postgresql
wget  http://stat-computing.org/dataexpo/2009/2008.csv.bz2
