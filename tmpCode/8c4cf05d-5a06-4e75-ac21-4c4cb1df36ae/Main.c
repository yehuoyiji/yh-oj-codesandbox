#include <stdio.h>  
#include <stdbool.h> // 引入stdbool.h头文件以使用bool类型  
  
// 声明isPalindrome函数，使其在main函数之前可见  
bool isPalindrome(int x);  
  
int main() {
    int num;
	printf("请输入一个整数: ");  
    scanf("%d", &num);    
    bool result = isPalindrome(num); // 调用isPalindrome函数并存储返回值  
    if (result) {  
        printf("%d 是回文数\n", num);  
    } else {  
        printf("%d 不是回文数\n", num);  
    }  
    return 0;  
}  
  
// 定义isPalindrome函数  
bool isPalindrome(int x) {  
    if (x < 0) {  
        return false;  
    }  
    long int sum = 0;  
    long int n = x;  
    while (n != 0) {  
        sum = sum * 10 + n % 10;  
        n = n / 10;  
    }  
    if (sum == x) {  
        return true;  
    } else {  
        return false;  
    }  
}