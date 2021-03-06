//
//  OKPINCodeViewController.m
//  OneKey
//
//  Created by xiaoliang on 2020/12/10.
//  Copyright © 2020 OneKey. All rights reserved.
//

#import "OKPINCodeViewController.h"
#import "OK_PassWordView.h"

@interface OKPINCodeViewController ()

@property (weak, nonatomic) IBOutlet UIScrollView *scrollView;
@property (weak, nonatomic) IBOutlet UIView *bgView;
@property (weak, nonatomic) IBOutlet UILabel *titleLabel;
@property (weak, nonatomic) IBOutlet UIImageView *iconImageView;
@property (weak, nonatomic) IBOutlet UIView *dotBgView;
@property (weak, nonatomic) IBOutlet UILabel *descLabel;
@property (weak, nonatomic) IBOutlet UIView *keyBoardBgView;
- (IBAction)keyBtnClick:(UIButton *)sender;

@property (weak, nonatomic) IBOutlet UIButton *confirmBtn;

@property (nonatomic, strong)OK_PassWordView *pwdView;

@property (nonatomic, copy)PINCodeComplete complete;

@end

@implementation OKPINCodeViewController
+ (instancetype)PINCodeViewController:(PINCodeComplete)complete
{
    OKPINCodeViewController *pinCodeVc = [[UIStoryboard storyboardWithName:@"Hardware" bundle:nil]instantiateViewControllerWithIdentifier:@"OKPINCodeViewController"];
    pinCodeVc.complete = complete;
    return pinCodeVc;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    [self stupUI];
}

- (void)stupUI
{
    self.title = MyLocalizedString(@"Check the PIN code", nil);
    self.iconImageView.image = [UIImage imageNamed:@"Rectangle"];
    if (self.titleLabelText) {
        self.titleLabel.text = self.titleLabelText;
    } else {
        self.titleLabel.text = MyLocalizedString(@"Enter your 6-digit password according to the PIN code location comparison table displayed on the device", nil);
    }
    self.descLabel.text = MyLocalizedString(@"The number keys on the phone change randomly every time. The PIN number is not retrievable. You must keep it in mind", nil);
    [self.confirmBtn setTitle:MyLocalizedString(@"confirm", nil) forState:UIControlStateNormal];
    self.pwdView.userInteractionEnabled = NO;

    
    OK_PassWordView *pwdView = [[OK_PassWordView alloc]initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH - 20 * 2, (SCREEN_WIDTH - 20 * 2 - 11 * 5)/6)];
    pwdView.showType = passShow3;
    pwdView.num = 6;
    pwdView.tintColor = [UIColor whiteColor];
    pwdView.textBlock = ^(NSString *str) {
        //NSLog(@"str == %@",str);
    };
    self.pwdView = pwdView;
    [pwdView show];
    [self.dotBgView addSubview:pwdView];
    
}


- (IBAction)keyBtnClick:(UIButton *)sender {
    NSMutableString *oldStr = [NSMutableString stringWithString:self.pwdView.textF.text];
    NSString *newStr = @"";
    if (sender.tag == 1012) {
        //删除
        if (oldStr.length>1) {
            newStr = [oldStr substringToIndex:(oldStr.length-1)];
        }else{
            newStr = [oldStr substringToIndex:0];
        }
    }else if (sender.tag == 1011){
        if (self.complete && oldStr.length == 6) {
            self.complete(oldStr);
        }else{
            [kTools tipMessage:MyLocalizedString(@"Enter your 6-digit password according to the PIN code location comparison table displayed on the device", nil)];
        }
    }else{
        [oldStr appendString:[NSString stringWithFormat:@"%zd",sender.tag - 1000]];
        newStr = oldStr;
    }
    if (newStr.length > 6) {
        return;
    }
    [self.pwdView textFieldChanged:newStr];
}
@end
