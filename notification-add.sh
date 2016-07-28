#!/bin/bash

notify_storage="./notifications"
count_file="$notify_storage/count"

mkdir -p $notify_storage

if [ ! -f $count_file ]; then
	echo "0" > $count_file
fi

current_count=(`cat $count_file`)
new_count=$(( $current_count + 1 ))

echo "Notification $new_count"

title=""

echo "Title at first: "
read title

echo "Now the contents: (press Ctrl+D when done)"
contents=$(cat)

echo "$title" > $notify_storage/$current_count
echo "$contents" >> $notify_storage/$current_count

echo $new_count > $count_file
