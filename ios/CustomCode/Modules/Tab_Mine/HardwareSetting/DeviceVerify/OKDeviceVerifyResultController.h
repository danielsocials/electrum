//
//  OKDeviceVerifyResultController.h
//  OneKey
//
//  Created by liuzj on 2021/1/14.
//  Copyright © 2021 Onekey. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface OKDeviceVerifyResultController : BaseViewController
@property (nonatomic, assign) BOOL isPassed;
@property (nonatomic, strong) NSString *name;
@property (nonatomic, copy) void(^doneCallback)(void);
+ (instancetype)controllerWithStoryboard;
@end

NS_ASSUME_NONNULL_END
