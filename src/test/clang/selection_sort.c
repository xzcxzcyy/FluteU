void se_test() {
  //选择排序实现
  //进行N-1轮选择
  int *a = (int *) 0;
  #define n 16
  for (int i = 0; i < n - 1; i++) {
    int min_index = i;
    //找出第i小的数所在的位置
    for (int j = i + 1; j < n; j++) {
      if (a[j] < a[min_index]) {
        min_index = j;
      }
    }
    //将第i小的数，放在第i个位置；如果刚好，就不用交换
    if (i != min_index) {
      int temp = a[i];
      a[i] = a[min_index];
      a[min_index] = temp;
    }
  }
}