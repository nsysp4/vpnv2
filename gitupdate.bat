git config --global core.autocrlf true
git pull origin master
git add -A
git commit -m "update"
git push origin master
git tag -a v6.1 -m "release v6.1"
git push origin --tags
pause